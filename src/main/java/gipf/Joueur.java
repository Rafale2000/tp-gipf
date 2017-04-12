package gipf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cette classe représente un joueur, avec son login, email, mot de passe et
 * score ELO. Les méthodes {@link #equals} et {@link #hashCode} sont redéfinies
 * pour que deux joueurs soient considérés comme égaux s'ils ont le même login.
 */
public class Joueur {
	private final String login;
	private String email;
	private String password;
	private double elo;

	/**
	 * Construit un joueur avec tous ses attributs
	 * 
	 * @param login
	 * @param email
	 * @param password
	 * @param elo
	 */
	public Joueur(String login, String email, String password, double elo) {
		super();
		this.login = login;
		this.email = email;
		this.password = password;
		this.elo = elo;
	}

	/**
	 * @return le score ELO du joueur
	 */
	public double getElo() {
		return elo;
	}

	/**
	 * Incrémente le score ELO du joueur (l'argument peut être négatif)
	 * 
	 * @param elo
	 */
	public void addElo(double elo) {
		this.elo += elo;
	}

	/**
	 * @return le login du joueur
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * Modifie le mot de passe du joueur
	 * 
	 * @param s
	 */
	public void setPassword(String s) {
		password = s;
	}

	/**
	 * Modifie l'adresse mail du joueur
	 * 
	 * @param s
	 */
	public void setEmail(String s) {
		email = s;
	}

	@Override
	public String toString() {
		return String.format("Joueur [login=%s, elo=%.0f]", login, elo);
	}

	/**
	 * Sauvegarde un joueur (ELO, mot de passe et email)
	 * 
	 * @param con
	 * @throws SQLException
	 */
	public void save(Connection con) throws SQLException {

		try (PreparedStatement stmt = con
				.prepareStatement("UPDATE Joueur SET elo = ?, password = ?, email = ? WHERE login = ?")) {
			stmt.setDouble(1, elo);
			stmt.setString(2, password);
			stmt.setString(3, email);
			stmt.setString(4, login);
			stmt.executeUpdate();
		}
	}

	/**
	 * Inscrit un joueur et l'enregistre en base de données Le score ELO est
	 * attribué à sa valeur par défaut
	 * 
	 * @param login
	 * @param password
	 * @param email
	 * @param con
	 * @return le joueur inscrit
	 * @throws SQLException
	 * @throws InscriptionException
	 *             si le login ou l'email existent déjà, si l'email ne comporte
	 *             pas le caractère '@' ou une autre contrainte d'intégrité n'a
	 *             pas été respectée
	 */
	public static Joueur inscrire(String login, String password, String email, Connection con)
			throws SQLException, InscriptionException {
		try (PreparedStatement stmt = con
				.prepareStatement("INSERT INTO Joueur VALUES (?, DEFAULT, ?, ?) RETURNING *")) {
			stmt.setString(1, login);
			stmt.setString(2, password);
			stmt.setString(3, email);
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) {
				throw new IllegalStateException("Aucune donnée insérée à l'inscription de " + login);
			}
			int elo = rs.getInt("elo");
			return new Joueur(login, email, password, elo);

		} catch (SQLException e) {
			switch (e.getSQLState()) {
			case "23505":
				throw new InscriptionException("Le login " + login + " ou l'email " + email + " existe déjà");
			case "23514":
				throw new InscriptionException("Erreur de contrôle des données : " + e.getLocalizedMessage());
			default:
				throw e;
			}
		}
	}

	/**
	 * Charge un joueur de la base à partir du login donné
	 * 
	 * @param login
	 * @param con
	 * @return un Optional contenant le joueur s'il existe
	 * @throws SQLException
	 */
	public static Optional<Joueur> load(String login, Connection con) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM Joueur WHERE login = ?")) {
			stmt.setString(1, login);
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) {
				return Optional.empty();
			} else {
				double elo = rs.getDouble("elo");
				String email = rs.getString("email");
				String pwd = rs.getString("password");
				return Optional.of(new Joueur(login, email, pwd, elo));
			}
		}
	}

	/**
	 * Recharge un joueur depuis la base
	 * 
	 * @param con
	 * @throws SQLException
	 */
	public void refresh(Connection con) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM Joueur WHERE login = ?")) {
			stmt.setString(1, login);
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) {
				throw new IllegalStateException();
			} else {
				elo = rs.getDouble("elo");
				email = rs.getString("email");
				password = rs.getString("password");
			}
		}
	}

	/**
	 * @param con
	 * @return la liste des joueurs de la base de données, classés par ELO
	 * @throws SQLException
	 */
	public static List<Joueur> loadByElo(Connection con) throws SQLException {
		try (Statement stmt = con.createStatement()) {
			ResultSet rs = stmt.executeQuery("SELECT * FROM Joueur ORDER BY elo DESC");
			ArrayList<Joueur> data = new ArrayList<>();
			while (rs.next()) {
				double elo = rs.getDouble("elo");
				String email = rs.getString("email");
				String pwd = rs.getString("password");
				String login = rs.getString("login");
				data.add(new Joueur(login, email, pwd, elo));
			}
			return data;
		}
	}

	/**
	 * @return l'email du joueur
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Contrôle la validité d'un mot de passe
	 * 
	 * @param p
	 * @return <tt>true</tt> si et seulement si <tt>p</tt> est le bon mot de
	 *         passe
	 */
	public boolean checkPassword(String p) {
		return password.equals(p);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Joueur) {
			return login.equals(((Joueur) o).login);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return login.hashCode();
	}

}
