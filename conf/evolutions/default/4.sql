# --- !Ups

create table tokens (
  email varchar PRIMARY KEY,
  token varchar not null
);

# --- !Downs

drop table tokens;
