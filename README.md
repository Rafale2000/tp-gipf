# tp-gipf
TP GIPF (DUT Informatique Maubeuge, M2106, problème 4)

On considère les classes *Joueur*, *Partie* et *Tournoi*.

On place quatre associations 1-\* entre *Joueur* et *Partie*, pour représenter respectivement le joueur blanc, noir, le gagnant et le perdant.

Les autres choix de conception sont évidents. 

L'outil Gradle permet de télécharger automatiquement le driver PostgreSQL. Il est supporté nativement par les versions récentes d'Eclipse.


* La branche *master* contient le squelette du TP (noms des classes et
des méthodes) à compléter. Des tests unitaires permettent de contrôler 
si votre code semble correct.
Commencez par créer votre base de données (vous pouvez utiliser le fichier `src/main/sql/create.sql`), incluant les éventuelles
contraintes et procédures stockées.
Complétez ensuite la classe `Main` : la méthode `connect` devra renvoyer
une connection à votre base de données de travail, et la méthode `clean`
devra la nettoyer (suppression de toutes les données).
Vous pouvez exécuter les tests unitaires dans Eclipse en faisant un clic-droit sur
le dossier `src/test/java` -> Run As -> JUnit Test.


* La branche *correction* contient le TP corrigé en utilisant des
Statement simples.


* La branche *correction-preparedStatements* contient un autre corrigé
utilisant des PreparedStatement partout où ils sont pertinents.
Attention, l'utilisation des PreparedStatement sera *obligatoire* le
jour du TP Contrôle.


Je vous invite à vous exercer en travaillant sur la branche "master" et
en complétant par vous-mêmes les méthodes demandées. La mise à jour des
scores ELO peut se faire purement un Java (comme dans le corrigé), ou
en utilisant des Triggers. 