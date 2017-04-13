DROP TABLE IF EXISTS Arbitre, Partie, Tournoi, Joueur;

-- Modèle relationnel (Question 1)

CREATE TABLE Joueur (
  login text PRIMARY KEY,
  elo real NOT NULL DEFAULT 1000,
  password text NOT NULL,
  email text NOT NULL UNIQUE CHECK (email ~ '@'));

CREATE TABLE Tournoi (
  idTournoi serial primary key,
  dateDebut date not null default now(),
  dateFin date,
  check (dateFin >= dateDebut),
  lieu text not null);

CREATE TABLE Partie (
  idPartie serial primary key,
  datePartie timestamp not null default now(),
  piecesRestantes int check (piecesRestantes >= 0),
  blanc text not null references Joueur,
  noir text not null references Joueur,
  gagnant text references Joueur,
  perdant text references Joueur,
  idTournoi int references Tournoi,
  check (blanc != noir),
  check (gagnant = noir or perdant = noir),
  check (gagnant = blanc or perdant = blanc));

CREATE TABLE Arbitre (
  login text references Joueur,
  idTournoi int references Tournoi,
  primary key (login, idTournoi));
  
CREATE OR REPLACE FUNCTION update_elo() RETURNS TRIGGER AS $$
DECLARE 
	score REAL;
	eloGagnant REAL;
	eloPerdant REAL;
BEGIN
	IF (NEW.gagnant IS NOT NULL AND NEW.perdant IS NOT NULL AND (OLD.gagnant IS NULL OR OLD.perdant IS NULL)) THEN
		SELECT INTO eloGagnant elo FROM Joueur WHERE login = NEW.gagnant;
		SELECT INTO eloPerdant elo FROM Joueur WHERE login = NEW.perdant;
		score := 32 * (1 - 1 / (1 + pow(10, (eloPerdant - eloGagnant) / 400)));
		UPDATE Joueur SET elo = elo + score WHERE login = NEW.gagnant;
		UPDATE joueur SET elo = elo - score WHERE login = NEW.perdant;
	END IF;
	RETURN NEW;	
END ;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_elo_trigger AFTER UPDATE ON Partie FOR EACH ROW EXECUTE PROCEDURE update_elo();