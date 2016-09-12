# --- !Ups

insert into people (id, name, email, phone, location, photo, description )
  values (1 , 'Nicolas Leroux' , 'nicolas.leroux@lunatech.com' , '+31618540795', 'rotterdam', '', 'Started at Lunatech as in intern, now owns it. Living the American dream exclusively to be able to afford the French dream, which involves a lot of alcohol.');

insert into people (id, name, email, phone, location, photo, description )
  values (2 , 'Anastasiia Pushkina' , 'anastasiia.pushkina@lunatech.com' , '+31618540795', 'rotterdam', '', 'Hi, my name is Anastasiia - short form is Nastya, if you can spell it) Originally I m from Ukraine, I moved to the Netherlands 4month ago and in Dutch I can say only -Ik werk in Lunatech-. I graduated from the University this summer with a degree in biology, so I m ready to join the team to cultivate stem cells. Kidding) A year ago I started to learn Java by my own: I took a few courses and my husband - Senior Java dev - helped me a lot, then I ve build a few small apps, and finally I m ready to work. It s my first job ever, so I m excited and nervous at the same time. My hobby is a sport orienteering (running with a map and a compass in a forest), I m into it for 10years. And I have so much more to tell, but I ll keep it for lunch breaks. Looking forward to getting to know you all.');


# --- !Downs

delete from people;
