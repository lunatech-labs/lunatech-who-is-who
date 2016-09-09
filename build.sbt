
name := """who-is-who"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

//resolvers ++= Seq("releases" at "http://repo.typesafe.com/typesafe/releases/",
//                    "snapshots" at "http://repo.typesafe.com/typesafe/snapshots",
//  "maven-releases" at "http://repo.typesafe.com/typesafe/maven-releases",
//  "third-party" at "http://repo.typesafe.com/typesafe/third-party",
//  "Lunatech Public Releases" at "http://artifactory.lunatech.com/artifactory/releases-public",
//  "Lunatech Snapshot Releases" at "http://artifactory.lunatech.com/artifactory/snapshots-public")
//
//resolvers += "Lunatech Artifactory" at "http://artifactory.lunatech.com/artifactory/releases-public"
resolvers += "Lunatech Artifactory" at "http://artifactory.lunatech.com/artifactory/snapshots"
//resolvers += Resolver.mavenLocal
//resolvers  ++= Seq(
//  "Apache repo" at "https://repository.apache.org/content/repositories/releases",
//  "Local Repo" at Path.userHome.asFile.toURI.toURL + "/.m2/repository",
//  Resolver.mavenLocal
//)

//resolvers += "Local Maven Repository" at "file:///"+Path.userHome+"/.ivy2/local/"

libraryDependencies ++= Seq( ws,
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.h2database" % "h2" % "1.4.190",
  "org.xerial" % "sqlite-jdbc" % "3.7.2",
//  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.lunatech" %% "play-googleopenconnect" % "1.2-SNAPSHOT",
//  "com.lunatech" %% "play-googleopenconnect" % "1.1",
//  "org.me" %% "play-googleopenconnect" % "1.2-SNAPSHOT",
    specs2 % Test

)



