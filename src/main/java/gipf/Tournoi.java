package gipf;

import static gipf.Main.escape;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Représente un tournoi
 */
public class Tournoi {
	private final int idTournoi;
	private final LocalDate debut;
	private Optional<LocalDate> fin;

	private final String lieu;

	private Set<Joueur> arbitres;

	private Tournoi(int idTournoi, LocalDate debut, Optional<LocalDate> fin, String lieu, List<Joueur> arbitres) {
		super();
		this.idTournoi = idTournoi;
		this.debut = debut;
		this.fin = fin;
		this.lieu = lieu;
		this.arbitres = new HashSet<Joueur>(arbitres);
	}

	/**
	 * @return la date de fin du tournoi si elle est connue
	 */
	public Optional<LocalDate> getFin() {
		return fin;
	}

	/**
	 * Modifie la date de fin
	 */
	public void setFin(LocalDate fin) {
		this.fin = Optional.of(fin);
	}

	/**
	 * 
	 * @return la liste des arbitres
	 */
	public Set<Joueur> getArbitres() {
		return Collections.unmodifiableSet(arbitres);
	}

	/**
	 * Ajoute un arbitre au tournoi
	 * 
	 * @param j
	 */
	public void addArbitre(Joueur j) {
		this.arbitres.add(j);
	}

	/**
	 * Supprime un arbitre du tournoi
	 * 
	 * @param j
	 */
	public void removeArbitre(Joueur j) {
		this.arbitres.remove(j);
	}

	/**
	 * @return la date de début du tournoi
	 */
	public LocalDate getDebut() {
		return debut;
	}

	/**
	 * 
	 * @return le lieu du tournoi
	 */
	public String getLieu() {
		return lieu;
	}

	/**
	 * Crée un tournoi avec les paramètres indiqués et l'enregistre dans la base
	 * 
	 * @param debut
	 * @param lieu
	 * @param arbitres
	 * @param con
	 * @return
	 * @throws SQLException
	 */
	public static Tournoi create(LocalDate debut, String lieu, List<Joueur> arbitres, Connection con)
			throws SQLException {
		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(
					"INSERT INTO Tournoi VALUES (DEFAULT, '" + debut + "', NULL, " + escape(lieu) + ") RETURNING *");
			if (!rs.next()) {
				throw new IllegalStateException(
						"Aucune donnée insérée à la création du tournoi de " + lieu + " du " + debut);
			}
			int id = rs.getInt("idTournoi");
			for (Joueur j : arbitres) {
				stmt.executeUpdate("INSERT INTO Arbitre VALUES (" + escape(j.getLogin()) + ", " + id + ")");
			}
			Tournoi t = new Tournoi(id, debut, Optional.empty(), lieu, arbitres);
			return t;
		}
	}

	/**
	 * Charge un tournoi depuis la base à partir de son identifiant
	 * 
	 * @param idTournoi
	 * @param con
	 * @return le tournoi s'il existe, les arbitres sont également chargés
	 * @throws SQLException
	 */
	public static Optional<Tournoi> load(int idTournoi, Connection con) throws SQLException {
		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery(
					"SELECT Tournoi.*, array_agg(login) filter (where login is not null) FROM Tournoi LEFT JOIN Arbitre USING (idTournoi) WHERE idTournoi = "
							+ idTournoi + " GROUP BY idTournoi");
			if (!rs.next()) {
				return Optional.empty();
			} else {
				LocalDate dateDebut = rs.getDate("dateDebut").toLocalDate();
				Optional<LocalDate> dateFin = Optional.ofNullable(rs.getDate("dateFin")).map(Date::toLocalDate);
				String lieu = rs.getString("lieu");
				Array loginArbitres = rs.getArray("array_agg");

				List<Joueur> arbitres = new ArrayList<Joueur>();
				if (loginArbitres != null) {
					for (String l : (String[]) loginArbitres.getArray()) {
						arbitres.add(Joueur.load(l, con).get());
					}
				}

				return Optional.of(new Tournoi(idTournoi, dateDebut, dateFin, lieu, arbitres));

			}
		}
	}

	/**
	 * Charge les parties d'un tournoi
	 * 
	 * @param con
	 * @return les parties chargées du tournoi
	 * @throws SQLException
	 */
	public List<Partie> loadParties(Connection con) throws SQLException {
		return Partie.loadTournoi(idTournoi, con);
	}

	@Override
	public String toString() {
		return "Tournoi [idTournoi=" + idTournoi + ", debut=" + debut + ", fin=" + fin + ", lieu=" + lieu
				+ ", arbitres=" + arbitres + "]";
	}

	/**
	 * 
	 * @return l'identifiant du tournoi
	 */
	public int getIdTournoi() {
		return idTournoi;
	}

	/**
	 * Sauvegarde le tournoi dans la base de données. La date de fin et la liste
	 * des arbitres peuvent avoir été modifiées.
	 * 
	 * @param con
	 * @throws SQLException
	 */
	public void save(Connection con) throws SQLException {
		try (Statement stmt = con.createStatement()) {
			final String f = fin.map(d -> "'" + d.toString() + "'").orElse("NULL");

			stmt.executeUpdate("UPDATE Tournoi SET dateFin = " + f + " WHERE idTournoi = " + idTournoi);
			stmt.executeUpdate("DELETE FROM Arbitre WHERE idTournoi = " + idTournoi);
			for (Joueur j : arbitres) {
				stmt.executeUpdate("INSERT INTO Arbitre VALUES (" + escape(j.getLogin()) + ", " + idTournoi + ")");
			}
		}
	}

}
