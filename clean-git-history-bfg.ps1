# Nettoyage de l’historique Git avec BFG Repo-Cleaner (Windows)

# 1. Télécharger BFG
# https://repo1.maven.org/maven2/com/madgag/bfg/1.14.0/bfg-1.14.0.jar

# 2. Ouvrir PowerShell dans le dossier parent de votre dépôt
# Remplacez D:/MISESERVICE/apps/MS-OVH-SMS par le chemin réel si besoin

# 3. Cloner le dépôt en mode miroir (pour préserver tous les refs)
git clone --mirror D:/MISESERVICE/apps/MS-OVH-SMS D:/MISESERVICE/apps/MS-OVH-SMS-mirror.git
cd D:/MISESERVICE/apps/MS-OVH-SMS-mirror.git

# 4. Nettoyer tous les .zip, .jar et le dossier gradle-8.5-bin de l’historique
java -jar ../bfg-1.14.0.jar --delete-files '*.zip' --delete-files '*.jar' --delete-folders gradle-8.5-bin

# 5. Nettoyer les objets orphelins
git reflog expire --expire=now --all
git gc --prune=now --aggressive

# 6. Forcer le push sur le dépôt distant (⚠️ réécrit l’historique)
git push --force

# 7. (Optionnel) Recloner le dépôt nettoyé pour repartir sur une base saine
# git clone https://github.com/<utilisateur>/<repo>.git

# Après cette opération, les fichiers volumineux seront définitivement supprimés de l’historique et le push fonctionnera sur GitHub.

