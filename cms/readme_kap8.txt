****************************************
****************************************
*Installation der Beispiele - Kapitel 8*
****************************************
*Voraussetzung ist die korrekte        *
*Installation von Tomcat, MySQL und Ant*
****************************************
*Beachten Sie bitte hierzu             *
*die Hinweise in Kapitel 3!            *
****************************************

1) Kopieren Sie das Verzeichnis kapitel8 auf die Festplatte 
(z.B. als C:\servlets\kapitel8 oder /home/user/servlets/kapitel8) 

2) Entfernen Sie den Schreibschutz von den Dateien. 
(Windows: attrib -R /s c:\kapitel8; 
Linux: chmod -R 777 /home/user/servlets/kapitel8) 

3) Starten Sie die Datenbank MySQL (mehr dazu steht in Kapitel 3) 

4) Verbinden Sie sich mit der Datenbank MySQL nach Anleitung 
aus Kapitel 3. Führen Sie folgende Befehle zum einrichten der 
Datenbank servlets und der Tabelle gaestebuch aus. Sie benötigen
diese Tabellen nur für das Beispiel gaestebuch. 

Unter Windows: 
mysql -u -p source c:\servlets\kapitel8\misc\servlets.sql; 
source c:\servlets\kapitel8\misc\create.sql; exit; 

Unter Linux: 
mysql -u -p < /home/user/servlets/kapitel8/servlets.sql 
mysql -u -p < /home/user/servlets/kapitel8/create.sql 

5) Passen Sie den Inhalt der Datei build.xml auf Ihre Pfade an. 
Achten Sie dabei darauf, unter Windows die Laufwerksangabe 
zu ergänzen (z.B. c:). Wichtig ist, dass Sie alle Pfade zu Tomcat, 
zur Servlet API ändern. 

6) Compilieren Sie die Anwendung mit Ant durch Aufruf 
von ant. Zur Installation und Konfiguration siehe Kapitel 3. 

7) Tragen Sie in die Datei tomcat-users.xml folgende Zeile ein:
<user name="cms" password="sagichnet" roles="admin"/>

8) Starten Sie Tomcat wie in Kapitel 3 beschrieben. 

9) Sie können nun folgende Beispiele aus ihrem Browser aufrufen:

http://localhost:8080/cms/news
oder http://localhost:8080/cms/news/admin 

Bei letzterer URL kommt eine Passwortabfrage auf welche Sie für 
den Benutzer 'cms' und als Passwort 'sagichnet' eingeben müssen, 
wie Sie es unter Punkt 7 eingetragen haben. 



*********************************************
*********************************************
**          Viel Spaß mit dem CMS          **
*********************************************
*********************************************
