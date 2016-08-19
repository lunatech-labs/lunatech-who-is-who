# --- !Ups

create table "people" (
--  "id" INTEGER PRIMARY KEY   AUTO_INCREMENT,
  "id" SERIAL PRIMARY KEY  ,
  "name" varchar not null ,
  "email" varchar not null,
  "photo" varchar not null,
  "description" varchar
);




# --- !Downs

drop table "people";


