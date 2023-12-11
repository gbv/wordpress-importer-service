# WordpressImporterService

A Service which synchronizes Wordpress blog posts with MyCoRe mods objects. This is the server part. For visualization you can use the [WordpressImporterGUI](https://github.com/gbv/wordpress-importer-gui).

## Development Server

Run `mvn clean jetty:run` to build the project and run a jetty web server.

## Configuration

The configuration should be present in the `.wpimport/` directory of the executing user. There should be a `config.json`.

### Example config.json
```json
{
  "parts": {
    "test": {
      "blog": "https://verfassungsblog.de/",
      "repository": "http://localhost:8291/mir/",
      "parentObject": "mir_mods_00000001",
      "postTemplate": "verfassungsblog.template.xml",
      "username": "admin",
      "password": "password",
      "license": "http://creativecommons.org/licenses/by/4.0/", 
      "license": {
        "label": "Public Domain Mark 1.0",
        "logoURL": "https://licensebuttons.net/l/publicdomain/80x15.png",
        "URL": "https://creativecommons.org/publicdomain/mark/1.0/",
        "classID": "cc_mark_1.0"
      }
    }
  }
}
```

* **blog** - the url to the WordPress instance
* **repository** - the url to the mycore instance
* **parentObject** - the id of the object to which all blogpost will be appended
* **postTemplate** - a file name or path relative to the `.wpimport` folder which contains a mods.xml template. 
* **username** - the username for the mycore restapi
* **password** - the password for the mycore restapi
* **license** - the license which will be used for all posts. Can be a string or an object with the following properties:
  * **label** - the label of the license
  * **logoURL** - the url to the license logo
  * **URL** - the url to the license
  * **classID** - the class id of the license (mods:classificiation/@authority=)

### Database

To check which wordpress post already has a mycore object, all childen of the `parentObject` will be loaded with the 
mycore restapi and all posts will be loaded with the wordpress restapi. Then every post which url is not found in a 
mycore object can be imported.

To load all MyCoRe objects and posts the service needs like 30 minutes. So i decided to save them in a small json based
file in the configuration folder and only update new ones. The files are named: `mycoredb_$hostname.json` and `blogdb_$hostname.json` 

