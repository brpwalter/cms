USE mysql;
INSERT INTO user (Host, User, Password) VALUES ("localhost","servlets", password("geheim"));
INSERT INTO db (Host, Db, User, Select_priv, Insert_priv, Update_priv, Delete_priv) VALUES ("%", "servlets", "servlets", 'Y', 'Y', 'Y', 'Y');
CREATE DATABASE servlets
