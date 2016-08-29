# --- !Ups


--be careful here
--we have line for sqlite (on local machine for development)
--and line for prostgres (on the cloud)
--choose the right one
create table "people" (
--  "id" INTEGER PRIMARY KEY   AUTOINCREMENT,
  "id" SERIAL PRIMARY KEY  ,
  "name" varchar not null ,
  "email" varchar not null,
  "location" INTEGER not null,
  "photo" varchar not null,
  "description" varchar
);




--be careful here
--we have line for sqlite (on local machine for development)
--and line for prostgres (on the cloud)
--choose the right one
create table "offices" (
--  "id" INTEGER PRIMARY Key  ,
  "id" integer PRIMARY Key ,
  "name" varchar not null ,
  "city" varchar not null,
  "country" varchar not null,
  "remarks" varchar
);




insert into "offices" values (1 , 'Rotterdam' , 'Rotterdam' , 'Netherlands' , 'Lunatech Headquarters');
insert into "offices" values (2 , 'Paris' , 'Montevrain' , 'France' , 'Lunatech main office in France');
insert into "offices" values (3 , 'Brussels' , 'Brussels' , 'Belgium' , 'Open in Future');
insert into "offices" values (0 , 'Non' , 'Non Specified' , 'Non Specified' , 'default value');

# --- !Downs



drop table "people";
drop table "offices";


