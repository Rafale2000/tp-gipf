package gipf;

import static com.github.npathai.hamcrestopt.OptionalMatchers.hasValue;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PartieTest {
	private static Connection con;

	@BeforeClass
	public static void connect() throws SQLException {
		con = Main.connect();
	}

	@AfterClass
	public static void close() throws SQLException {
		con.close();
	}

	private List<Joueur> joueurs;

	private List<Partie> parties;

	@Before
	public void setUp() throws SQLException {
		Main.clean(con);
		joueurs = JoueurTest.inscrire(con);

		parties = randParties(joueurs, new Random(0), con);
		parties.get(0).setGagnant(true, 4, con);
	}

	public static List<Partie> randParties(List<Joueur> joueurs, Random rand, Connection con) throws SQLException {
		List<Partie> parties = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			int j1 = rand.nextInt(joueurs.size());
			int j2 = rand.nextInt(joueurs.size());
			if (j1 != j2) {
				parties.add(Partie.create(joueurs.get(j1), joueurs.get(j2), con));
			}
		}
		return parties;
	}

	public static void randGagnants(List<Partie> parties, Random rand, Connection con) throws SQLException {
		for (Partie p : parties) {
			p.setGagnant(rand.nextBoolean(), rand.nextInt(5), con);
		}
	}

	@Test
	public void testLoad() throws SQLException {
		randGagnants(parties.subList(1, 30), new Random(0), con);
		for (Partie tp : parties) {
			Partie p = Partie.load(tp.getIdPartie(), con).get();

			assertEquals(tp.getIdPartie(), p.getIdPartie());
			assertEquals(tp.getDate(), p.getDate());

			Joueur blanc = p.getBlanc();
			Joueur noir = p.getNoir();
			assertEquals(tp.getBlanc().getLogin(), blanc.getLogin());
			assertEquals(tp.getNoir().getLogin(), noir.getLogin());
			assertEquals(tp.getGagnant(), p.getGagnant());
			assertEquals(tp.getPerdant(), p.getPerdant());
			p.getGagnant().ifPresent(j -> assertThat(j, either(sameInstance(blanc)).or(sameInstance(noir))));
			p.getPerdant().ifPresent(j -> assertThat(j, either(sameInstance(blanc)).or(sameInstance(noir))));
			assertEquals(tp.getPiecesRestantes(), p.getPiecesRestantes());
			assertThat(p.getIdTournoi(), isEmpty());
		}
	}

	@Test
	public void testSave() throws SQLException {
		Partie p = parties.get(0);
		p.setDate(LocalDateTime.of(2001, 7, 3, 11, 32));
		p.save(con);

		Partie l = Partie.load(p.getIdPartie(), con).get();
		assertEquals(p.getDate(), l.getDate());
	}

	@Test(expected = Exception.class)
	public void testSetGagnantDouble() throws SQLException {
		Partie p = parties.get(0);
		p.setGagnant(true, 4, con);
	}

	@Test
	public void testSetGagnant() throws SQLException {

		Partie p = parties.get(1);
		Joueur blanc = p.getBlanc();
		Joueur noir = p.getNoir();

		// Contrôles initialisation
		assertThat(p.getIdPartie(), greaterThan(0));
		assertThat(p.getGagnant(), isEmpty());
		assertThat(p.getPerdant(), isEmpty());
		assertThat(p.getPiecesRestantes(), isEmpty());

		assertEquals(1000, blanc.getElo(), 0);
		assertEquals(1000, noir.getElo(), 0);

		p.setGagnant(true, 5, con);

		// Contrôle mise à jour en RAM
		assertThat(p.getGagnant(), hasValue(blanc));
		assertThat(p.getPerdant(), hasValue(noir));
		assertThat(p.getPiecesRestantes(), hasValue(5));

		assertEquals(1016, blanc.getElo(), .5);
		assertEquals(984, noir.getElo(), .5);

		// Contrôle mise à jour en BDD
		Partie loaded = Partie.load(p.getIdPartie(), con).get();
		assertThat(loaded.getGagnant(), hasValue(blanc));
		assertThat(loaded.getPerdant(), hasValue(noir));
		assertThat(loaded.getPiecesRestantes(), hasValue(5));

		Joueur lBlanc = Joueur.load(blanc.getLogin(), con).get();
		assertEquals(1016, lBlanc.getElo(), .5);
		Joueur lNoir = Joueur.load(noir.getLogin(), con).get();
		assertEquals(984, lNoir.getElo(), .5);

		// Tests algorithme ELO sur une série de parties
		randGagnants(parties.subList(2, 30), new Random(0), con);

		Map<String, Integer> testValues = new HashMap<>();
		testValues.put("baroqueen", 969);
		testValues.put("cobrag", 967);
		testValues.put("vikingkong", 955);
		testValues.put("preaster", 1043);
		testValues.put("fickleSkeleton", 1015);
		testValues.put("SnowTea", 1060);
		testValues.put("AfternoonTerror", 1044);
		testValues.put("JokeCherry", 914);
		testValues.put("JealousPelican", 1018);
		testValues.put("PositiveLamb", 1014);

		// for (Joueur j : joueurs) {
		// System.out.println(j.toString()));
		// }

		for (Joueur j : joueurs) {
			assertEquals(testValues.get(j.getLogin()), j.getElo(), .5);
		}

	}

	@Test
	public void testPartiesJouees() throws SQLException {
		Map<String, Integer> testValues = new HashMap<>();
		testValues.put("baroqueen", 8);
		testValues.put("cobrag", 5);
		testValues.put("vikingkong", 8);
		testValues.put("preaster", 11);
		testValues.put("fickleSkeleton", 6);
		testValues.put("SnowTea", 11);
		testValues.put("AfternoonTerror", 4);
		testValues.put("JokeCherry", 13);
		testValues.put("JealousPelican", 11);
		testValues.put("PositiveLamb", 5);

		Map<Joueur, Integer> classement = Partie.classementPartiesJouees(con);
		assertThat(classement.keySet(), containsInAnyOrder(joueurs.toArray()));
		int prev = Integer.MAX_VALUE;
		for (Entry<Joueur, Integer> j : classement.entrySet()) {
			assertEquals(testValues.get(j.getKey().getLogin()), j.getValue());
			assertThat(prev, greaterThanOrEqualTo(j.getValue()));
			prev = j.getValue();
		}
	}

	@Test
	public void testPartiesGagnees() throws SQLException {
		randGagnants(parties.subList(1, 30), new Random(0), con);
		Map<String, Integer> testValues = new HashMap<>();
		testValues.put("baroqueen", 4);
		testValues.put("cobrag", 2);
		testValues.put("vikingkong", 2);
		testValues.put("preaster", 5);
		testValues.put("fickleSkeleton", 2);
		testValues.put("SnowTea", 5);
		testValues.put("AfternoonTerror", 0);
		testValues.put("JokeCherry", 5);
		testValues.put("JealousPelican", 3);
		testValues.put("PositiveLamb", 2);

		Map<Joueur, Integer> classement = Partie.classementPartiesGagnees(con);

		assertThat(classement.keySet(), containsInAnyOrder(joueurs.toArray()));
		int prev = Integer.MAX_VALUE;
		for (Entry<Joueur, Integer> j : classement.entrySet()) {
			assertEquals(testValues.get(j.getKey().getLogin()), j.getValue());
			assertThat(prev, greaterThanOrEqualTo(j.getValue()));
			prev = j.getValue();
		}
	}
}
