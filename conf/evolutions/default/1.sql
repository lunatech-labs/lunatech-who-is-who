# --- !Ups

create table people (
  id SERIAL PRIMARY KEY,
  name varchar not null,
  email varchar not null,
  phone varchar not null,
  location varchar not null,
  photo varchar,
  description text
);

# --- !Downs

drop table people;
