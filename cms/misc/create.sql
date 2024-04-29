USE servlets;

CREATE TABLE news (
	id		int	AUTO_INCREMENT PRIMARY KEY,
	category	int,
	news_date	date,
	release_date	date,
	display		int,
	headline	text,
	content		text,
	image		longblob,
	imagetype	varchar(255),
	imagetext	text,
	download	longblob,
        downloadtype    varchar(255),
        downloadname    varchar(255),
	downloadtext	text

);
