# Who Is Who

This web app allow users to have simple list of employees along with their information such as email, location, photo, etc.
It is supposed to be easy to install and use, also, to modify and extend.


# REST interface

GET     /api/persons                controllers.PersonController.getPersons
GET     /photo/:name                controllers.PersonController.giveMePicture(name: String)
GET     /api/person/:email          controllers.PersonController.getSinglePerson(email: String)
POST    /api/person/edit            controllers.PersonController.jsonEditPerson


whoiswho.cleverapps.io/api/persons
[
  {
    "id": 1,
    "name": "Ali Hassan",
    "email": "ali.hassan@lunatech.com",
    "location": {
      "id": 2,
      "name": "Paris",
      "city": "Montevrain",
      "country": "France",
      "remarks": "Lunatech main office in France"
    },
    "photo": "file000541344089.jpg",
    "description": "Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. "
  }
]


whoiswho.cleverapps.io/api/person/ali.hassan@lunatech.com

{
  "id": 1,
  "name": "Ali Hassan",
  "email": "ali.hassan@lunatech.com",
  "location": {
    "id": 2,
    "name": "Paris",
    "city": "Montevrain",
    "country": "France",
    "remarks": "Lunatech main office in France"
  },
  "photo": "file000541344089.jpg",
  "description": "Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. Started working at Lunatech in 2016. "
}


whoiswho.cleverapps.io/api/person/edit
{"id":16,"name":"Ali Hassan","email":"ali.hassan@lunatech.com","description":"changed from json"}
{
  "status": "OK",
  "message": "Person 'Ali Hassan' saved."
}

From command line:
curl --request POST  --header "Content-type: application/json"   --data '{"id":16,"name":"Ali Hassan","email":"ali.hassan@lunatech.com","description":"changed from json"}'   http://whoiswho.cleverapps.io/api/person/edit




DB
pictures
