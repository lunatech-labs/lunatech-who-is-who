# --- !Ups

create table "people" (
  "id" INTEGER PRIMARY KEY   AUTOINCREMENT,
  "name" varchar not null ,
  "email" varchar not null,
  "photo" varchar not null,
  "description" varchar
);

# --- !Downs

drop table "people" if exists;


