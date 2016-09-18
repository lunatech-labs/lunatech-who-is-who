# --- !Ups

alter table people add role varchar;

update people set role = 'Developer';

alter table people alter role SET not null;

# --- !Downs

alter table people drop column role;
