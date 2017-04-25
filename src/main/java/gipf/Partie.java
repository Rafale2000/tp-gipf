package gipf;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Représente une partie, jouée ou simplement programmée, éventuellement liée à
 * un tournoi
 */
public class Partie {

	private final int idPartie;
	private LocalDateTime date;
	private Integer piecesRestantes;

	private final Joueur blanc;
	private final Joueur noir;
	private Joueur gagnant;
	private Joueur perdant;

	private Integer idTournoi;

	private Partie(int idPartie, LocalDateTime date, Joueur blanc, Joueur noir, Joueur gagnant, Joueur perdant,
			Integer piecesRestantes, Integer idTournoi) {
		super();
		this.idPartie = idPartie;
		this.date = date;
		this.piecesRestantes = piecesRestantes;
		this.blanc = blanc;
		this.noir = noir;
		this.gagnant = gagnant;
		this.perdant = perdant;
		this.idTournoi = idTournoi;
	}

	/**
	 * @return l'identifiant de la partie dans la base de données
	 */
	public int getIdPartie() {
		return idPartie;
	}

	/**
	 * @return date et heure de la partie
	 */
	public LocalDateTime getDate() {
		return date;
	}

	/**
	 * Modifie les date et heure de la partie
	 */
	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	/**
	 * 
	 * @return le nombre de pièces restantes à l'issue de la partie s'il est
	 *         connu
	 */
	public Optional<Integer> getPiecesRestantes() {
		return Optional.ofNullable(piecesRestantes);
	}

	/**
	 * 
	 * @return le joueur blanc
	 */
	public Joueur getBlanc() {
		return blanc;
	}

	/**
	 * 
	 * @return le joueur noir
	 */
	public Joueur getNoir() {
		return noir;
	}

	/**
	 * 
	 * @return le joueur gagnant s'il est connu (même instance que le blanc ou
	 *         le noir)
	 */
	public Optional<Joueur> getGagnant() {
		return Optional.ofNullable(gagnant);
	}

	/**
	 * 
	 * @return le joueur perdant s'il est connu (même instance que le blanc ou
	 *         le noir)
	 */
	public Optional<Joueur> getPerdant() {
		return Optional.ofNullable(perdant);
	}

	/**
	 * 
	 * @return l'identifiant du tournoi si la partie est liée à celui-ci
	 */
	public Optional<Integer> getIdTournoi() {
		return Optional.ofNullable(idTournoi);
	}

	@Override
	public String toString() {
		return "Partie [idPartie=" + idPartie + ", date=" + date + ", piecesRestantes=" + piecesRestantes + ", blanc="
				+ blanc + ", noir=" + noir + ", gagnant=" + gagnant + ", perdant=" + perdant + "]";
	}

	/**
	 * Crée une partie à l'issue inconnue à l'heure par défaut et l'enregistre
	 * en base de données.
	 * 
	 * @param blanc
	 * @param noir
	 * @param con
	 * @return La partie générée avec son identifiant correct
	 * @throws SQLException
	 */
	public static Partie create(Joueur blanc, Joueur noir, Connection con) throws SQLException {
		try (Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("INSERT INTO Partie VALUES (DEFAULT, DEFAULT, NULL, '"
						+ blanc.getLogin() + "', '" + noir.getLogin() + "', NULL, NULL, NULL) RETURNING *")) {
			if (!rs.next()) {
				throw new IllegalStateException("Aucune donnée insérée à l'enregistrement de la partie");
			}
			int id = rs.getInt("idPartie");
			LocalDateTime date = rs.getTimestamp("datePartie").toLocalDateTime();
			return new Partie(id, date, blanc, noir, null, null, null, null);
		}
	}

	/**
	 * Inscrit la partie dans un tournoi
	 * 
	 * @param t
	 */
	public void setTournoi(Tournoi t) {
		idTournoi = t.getIdTournoi();
	}

	/**
	 * Charge une partie à partir de son identifiant
	 * 
	 * @param idPartie
	 * @param con
	 * @return la partie si elle existe
	 * @throws SQLException
	 */
	public static Optional<Partie> load(int idPartie, Connection con) throws SQLException {
		try (Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM Partie WHERE idPartie = " + idPartie)) {
			return load(rs, con).stream().findAny();
		}
	}

	private static List<Partie> load(ResultSet rs, Connection con) throws SQLException {
		List<Partie> parties = new ArrayList<>();
		while (rs.next()) {
			int idPartie = rs.getInt("idPartie");
			LocalDateTime date = rs.getTimestamp("datePartie").toLocalDateTime();// toInstant();
			String loginBlanc = rs.getString("blanc");
			String loginNoir = rs.getString("noir");

			// Note: le type Integer permet de gérer les valeurs "null",
			// contairement au type primitif "int"
			Integer piecesRestantes = (Integer) rs.getObject("piecesRestantes");

			String loginGagnant = rs.getString("gagnant");
			String loginPerdant = rs.getString("perdant");
			Integer idTournoi = (Integer) rs.getObject("idTournoi");

			Joueur blanc = Joueur.load(loginBlanc, con).get();
			Joueur noir = Joueur.load(loginNoir, con).get();

			Joueur gagnant = null;
			Joueur perdant = null;
			if (loginGagnant != null) {
				assert loginPerdant != null;
				gagnant = loginGagnant.equals(loginBlanc) ? blanc : noir;
				perdant = loginPerdant.equals(loginBlanc) ? blanc : noir;
			}

			parties.add(new Partie(idPartie, date, blanc, noir, gagnant, perdant, piecesRestantes, idTournoi));
		}
		return parties;
	}

	/**
	 * Charge une liste de parties à partir de l'identifiant d'un tournoi
	 * 
	 * @param idTournoi
	 * @param con
	 * @return la liste de toutes les parties liées au tournoi donné
	 * @throws SQLException
	 */
	public static List<Partie> loadTournoi(int idTournoi, Connection con) throws SQLException {
		try (Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT * FROM Partie WHERE idTournoi = " + idTournoi)) {
			return load(rs, con);
		}
	}

	/**
	 * 
	 * @param con
	 * @return un classement des joueurs en fonction du nombre de parties
	 *         jouées. Un LinkedHashMap permet de lier le nombre de parties
	 *         jouées à chaque joueur tout en conservant le classement.
	 * @throws SQLException
	 */
	public static LinkedHashMap<Joueur, Integer> classementPartiesJouees(Connection con) throws SQLException {
		try (Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery("WITH Played AS (                    "
						+ "  SELECT idPartie, blanc AS login FROM Partie              "
						+ "  UNION                                                    "
						+ "  SELECT idPartie, noir AS login FROM Partie)              "
						+ "SELECT Joueur.*, count(idPartie)                           "
						+ "FROM Joueur LEFT JOIN Played USING (login)                 "
						+ "GROUP BY login                                             "
						+ "ORDER BY count(idPartie) DESC                              ")) {

			LinkedHashMap<Joueur, Integer> data = new LinkedHashMap<>();
			while (rs.next()) {
				int elo = rs.getInt("elo");
				String email = rs.getString("email");
				String pwd = rs.getString("password");
				String login = rs.getString("login");
				int count = rs.getInt("count");
				data.put(new Joueur(login, email, pwd, elo), count);
			}
			return data;
		}
	}

	/**
	 * 
	 * @param con
	 * @return un classement des joueurs en fonction du nombre de parties
	 *         gagnées. Un LinkedHashMap permet de lier le nombre de parties
	 *         gagnées à chaque joueur tout en conservant le classement.
	 * @throws SQLException
	 */
	public static LinkedHashMap<Joueur, Integer> classementPartiesGagnees(Connection con) throws SQLException {
		try (Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery(
						"SELECT Joueur.*, count(idPartie) FROM Joueur LEFT JOIN Partie ON login = gagnant "
								+ "GROUP BY login ORDER BY count(idPartie) DESC")) {
			LinkedHashMap<Joueur, Integer> data = new LinkedHashMap<>();
			while (rs.next()) {
				int elo = rs.getInt("elo");
				String email = rs.getString("email");
				String pwd = rs.getString("password");
				String login = rs.getString("login");
				int count = rs.getInt("count");
				data.put(new Joueur(login, email, pwd, elo), count);
			}
			return data;
		}
	}

	/**
	 * Sauvegarde les modifications d'une partie dans la base. On ne sauvegarde
	 * que la date de la partie et l'éventuel identifiant du tournoi. Pour
	 * indiquer le perdant ou le gagnant, utiliser {@link #setGagnant}.
	 * 
	 * @param con
	 * @throws SQLException
	 */
	public void save(Connection con) throws SQLException {
		try (Statement stmt = con.createStatement()) {
//			final String tourn = idTournoi.toString();
//			if (idTournoi == null) {
//				tourn = "NULL";
//			} else {
//				tourn = idTournoiInteger.toString(idTournoi);
//			}

			stmt.executeUpdate("UPDATE Partie SET datePartie = '" + date + "', idTournoi = " + idTournoi
					+ " WHERE idPartie = " + idPartie);
		}
	}

	/**
	 * Indique le gagnant d'une partie à l'aide du booléen <tt>blancGagne</tt>,
	 * ainsi que le nombre de pièces restantes. Le résultat est enregistré dans
	 * la base et les scores ELO des deux joueurs sont mis à jour et
	 * sauvegardés.
	 * 
	 * @param blancGagne
	 * @param piecesRestantes
	 * @param con
	 * @throws SQLException
	 */
	public void setGagnant(boolean blancGagne, int piecesRestantes, Connection con) throws SQLException {
		if (this.piecesRestantes != null) {
			throw new IllegalStateException("Le vainqueur de cette partie a déjà été désigné");
		}
		final Joueur g;
		final Joueur p;
		if (blancGagne) {
			g = blanc;
			p = noir;
		} else {
			g = noir;
			p = blanc;
		}
		gagnant = g;
		perdant = p;
		this.piecesRestantes = piecesRestantes;

		try (Statement stmt = con.createStatement()) {
			stmt.executeUpdate("UPDATE Partie SET gagnant = '" + g.getLogin() + "', perdant = '" + p.getLogin()
					+ "', piecesRestantes = '" + piecesRestantes + "' WHERE idPartie = " + idPartie);
		}

		final double score = 32 * (1 - 1 / (1 + Math.pow(10, (p.getElo() - g.getElo()) / 400)));
		g.addElo(score);
		p.addElo(-score);
		g.save(con);
		p.save(con);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Partie) {
			return idPartie == ((Partie) o).idPartie;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return idPartie;
	}

}
