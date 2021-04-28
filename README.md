# feature-api

## Installatie
* In server.xml moet de jndi resource staan voor de jdbc/geo_viewer
   <Resource
  name="jdbc/geo_viewer"
  auth="Container"
  type="javax.sql.DataSource"
  username="<USERNAM>"
  password="<PASSWORD>"
  driverClassName="org.postgresql.Driver"
  url="jdbc:postgresql://localhost:5432/flamingo" 
  maxActive="40"
  validationQuery="select 1"
  timeBetweenEvictionRunsMillis="30000"
  minEvictableIdleTimeMillis="5000"
/>

* In tomcat/conf/context.xml moet de resourcelink staan
    <ResourceLink 
  name="jdbc/geo_viewer" 
  global="jdbc/geo_viewer" 
  type="javax.sql.DataSource"/>
