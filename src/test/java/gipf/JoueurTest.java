package gipf;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JoueurTest {

	public static List<String> usernames = Arrays.asList("baroqueen", "cobrag", "vikingkong", "preaster",
			"fickleSkeleton", "SnowTea", "AfternoonTerror", "JokeCherry", "JealousPelican", "PositiveLamb");

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

	@Before
	public void setUp() throws SQLException {
		Main.clean(con);
		joueurs = inscrire(con);
	}

	public static List<Joueur> inscrire(Connection con) {
		return usernames.stream().map(username -> {
			try {
				return Joueur.inscrire(username, UUID.randomUUID().toString(), username + "@univ-valenciennes.fr", con);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}).collect(Collectors.toList());
	}

	@Test
	public void testEquals() {
		Joueur j1 = new Joueur("baroqueen", "", "", 0);
		assertEquals(j1, joueurs.get(0));
	}

	@Test
	public void testInscrire() {
		for (int i = 0; i < usernames.size(); i++) {
			assertEquals(usernames.get(i), joueurs.get(i).getLogin());
			assertEquals(1000, joueurs.get(i).getElo(), 0);
			assertEquals(usernames.get(i) + "@univ-valenciennes.fr", joueurs.get(i).getEmail());
		}
	}

	@Test(expected = InscriptionException.class)
	public void testInscrireDoublon() throws SQLException, InscriptionException {
		String un = usernames.get(0);
		Joueur.inscrire(un, UUID.randomUUID().toString(), "toto@univ-valenciennes.fr", con);
	}

	@Test(expected = InscriptionException.class)
	public void testInscrireBadMail() throws SQLException, InscriptionException {
		Joueur.inscrire("toto", UUID.randomUUID().toString(), "univ-valenciennes.fr", con);
	}

	@Test(expected = InscriptionException.class)
	public void testInscrireDoubleMail() throws SQLException, InscriptionException {
		Joueur.inscrire("toto", UUID.randomUUID().toString(), usernames.get(0) + "@univ-valenciennes.fr", con);
	}

	@Test
	public void testInscrireQuote() throws SQLException, InscriptionException {
		Joueur.inscrire("to'to", "to'to123", "to%27to@univ-valenciennes.fr", con);
		Joueur j = Joueur.load("to'to", con).get();
		assertEquals("to'to", j.getLogin());
		assertEquals("to%27to@univ-valenciennes.fr", j.getEmail());
		assertTrue(j.checkPassword("to'to123"));
	}

	@Test(expected = Exception.class)
	public void testBadSave() throws SQLException {
		Joueur j = joueurs.get(0);
		j.setEmail(joueurs.get(1).getEmail());
		j.save(con);
		j.setEmail("toto");
		j.save(con);
	}

	@Test
	public void testSaveQuote() throws SQLException {
		Joueur j = joueurs.get(0);
		j.setPassword("to'to123");
		j.save(con);
	}

	@Test
	public void testInjection() throws SQLException {
		Joueur j = joueurs.get(0);

		Joueur attaque = joueurs.get(1);

		// L'attaque consiste à changer l'email d'un autre utilisateur (ici
		// "cobrag" au lieu du joueur j)
		// pour ensuite demander par exemple une récupération de mot de passe...

		try {
			j.setEmail("hackerz@hotmail.com' WHERE login = '" + attaque.getLogin() + "' --");
			j.save(con);
		} catch (Exception e) {
			// Il peut y avoir exception ici si l'attaque est détectée
		}

		// Quoi qu'il en soit l'email de cobrag ne doit pas avoir été modifié
		Joueur j2 = Joueur.load(attaque.getLogin(), con).get();
		assertEquals(attaque.getEmail(), j2.getEmail());

	}

	@Test
	public void testLoad() throws SQLException {
		for (String u : usernames) {
			Optional<Joueur> gj = Joueur.load(u, con);
			assertThat(gj, isPresent());
			Joueur j = gj.get();
			assertEquals(u, j.getLogin());
			assertEquals(1000, j.getElo(), 0);
			assertEquals(u + "@univ-valenciennes.fr", j.getEmail());
		}
		assertThat(Joueur.load("toto", con), isEmpty());
	}

	@Test
	public void testElo() throws SQLException {
		Joueur j = joueurs.get(0);
		j.addElo(1000);
		assertEquals(2000, j.getElo(), 0);
		j.addElo(-20);
		assertEquals(1980, j.getElo(), 0);
	}

	@Test
	public void testSave() throws SQLException {
		Joueur j = joueurs.get(0);
		j.setPassword("newPass");
		j.addElo(1000);
		j.setEmail("a@b.com");
		j.save(con);

		Joueur loaded = Joueur.load(usernames.get(0), con).get();
		assertTrue(loaded.checkPassword("newPass"));
		assertEquals(2000, loaded.getElo(), 0);
		assertEquals("a@b.com", loaded.getEmail());
	}

	@Test
	public void testClassementElo() throws SQLException {
		Random rand = new Random(0);
		List<Partie> parties = PartieTest.randParties(joueurs, rand, con);
		PartieTest.randGagnants(parties, rand, con);

		Map<String, Double> refElo = joueurs.stream().collect(Collectors.toMap(Joueur::getLogin, Joueur::getElo));

		List<Joueur> classement = Joueur.loadByElo(con);
		assertThat(classement, containsInAnyOrder(joueurs.toArray()));

		for (Joueur j : classement) {
			assertEquals(refElo.get(j.getLogin()), j.getElo(), 0.5);
		}

		for (int i = 0; i < classement.size() - 1; i++) {
			assertThat(classement.get(i).getElo(), greaterThanOrEqualTo(classement.get(i + 1).getElo()));
		}
	}

}
