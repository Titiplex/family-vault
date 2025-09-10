plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "3.1.3"
}

group = "com.titiplex"
version = "1.0.1"

repositories {
    mavenCentral()
}

val junitVersion = "5.10.2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(23)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("FamilyHub.main")
    mainClass.set("com.titiplex.familyhub.Main")
}

javafx {
    version = "23.0.1"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.web", "javafx.swing", "javafx.media")
}

dependencies {
    // https://mvnrepository.com/artifact/org.flywaydb/flyway-core
    implementation("org.flywaydb:flyway-core:10.22.0")
    // https://mvnrepository.com/artifact/at.favre.lib/bcrypt
    implementation("at.favre.lib:bcrypt:0.10.2")
    // https://mvnrepository.com/artifact/org.bitlet/weupnp
    implementation("org.bitlet:weupnp:0.1.4")
    // https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("org.flywaydb:flyway-community-db-support:10.22.0")
    // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api
    implementation("org.apache.logging.log4j:log4j-api:2.25.1")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.25.1")

    implementation("org.controlsfx:controlsfx:11.2.1")
    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") {
        exclude(group = "org.openjfx")
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
    implementation("net.synedra:validatorfx:0.5.0") {
        exclude(group = "org.openjfx")
    }
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")
    implementation("eu.hansolo:tilesfx:21.0.3") {
        exclude(group = "org.openjfx")
    }
    implementation("com.github.almasb:fxgl:17.3") {
        exclude(group = "org.openjfx")
        exclude(group = "org.jetbrains.kotlin")
    }
    // implementation("org.xerial:sqlite-jdbc:3.50.2.0")
    implementation("com.h2database:h2:2.3.232")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")

    implementation("com.github.librepdf:openpdf:1.3.32")

    implementation("org.apache.poi:poi-ooxml:5.4.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress=2", "--no-header-files", "--no-man-pages"))

    forceMerge("flyway-core", "bcrypt", "weupnp", "sqlite-jdbc")

    addExtraDependencies("org.apache.logging.log4j:log4j-api:2.25.1")

    mergedModule {
        requires("java.sql")
        // (souvent utile aussi avec les libs de log)
        requires("java.logging")
        requires("jdk.unsupported")  // Unsafe utilisé par pas mal de libs (sqlite-jdbc, etc.)
        requires("jdk.crypto.ec")    // TLS/HTTPS (certs EC) sinon SSL peut casser
        requires("java.xml")         // XML (Flyway, Log4j configs, etc.)
        requires("java.naming")      // JNDI utilisé indirectement par certaines libs
        uses("java.sql.Driver")                          // pour sqlite-jdbc
        // Flyway utilise des plugins via ServiceLoader : on couvre l’API publique
        uses("org.flywaydb.core.extensibility.Plugin")   // safe, côté API Flyway
        // Tu peux en ajouter d'autres si besoin apparaît dans les logs :
        // uses("org.flywaydb.core.api.logging.LogCreator")
    }

    launcher {
        name = "FamilyHub"
    }
    /**
     * jpackage (installateur)
     * Génère un MSI auto-contenu : pas besoin de JRE installé chez l’utilisateur.
     */
    jpackage {
        // Windows : "msi" (ou "exe")
        installerType = "exe"

        // Nom affiché et nom interne de l’app-image
        imageName = "FamilyHub"

        // Version app/installeur
        // [!!] jpackage exige une version purement numérique
        // si tu veux garder project.version = "1.0-SNAPSHOT", fais plutôt :
        // appVersion = project.version.toString().replace(Regex("[^0-9.]"), "").ifBlank { "1.0.0" }
        appVersion = project.version.toString()

        // Éditeur
        vendor = "Titiplex"

        // Icône et ressources (README, LICENSE…)
        icon = file("src/main/resources/installer/familyhub.ico").absolutePath
        resourceDir = file("src/main/resources/installer")

        // Options Windows
        installerOptions = listOf(
            "--win-dir-chooser",          // l’utilisateur peut choisir le dossier d’install
            "--win-per-user-install",     // pas besoin d’admin (installe dans AppData)
            "--win-menu",                 // entrée Menu Démarrer
            "--win-menu-group", "FamilyHub",
            "--win-shortcut",             // raccourci Bureau
            "--win-shortcut-prompt"       // propose la création de raccourcis pendant l’install
        )

        // Description de l’app dans l’installeur
        installerOptions = installerOptions + listOf("--description", "Application de gestion de famille – FamilyHub")

        // Si ton app a besoin d'autres fichiers (ex: logos, modèles PDF…),
        // place-les dans src/installer/app/ ; ils seront copiés à côté de l’exécutable.
        // Exemple : src/installer/app/images/logo.png
    }
}

/**
 * Tâche de confort pour générer directement l’installateur
 * (équivalent à `gradlew jpackage`).
 */
tasks.register("makeInstaller") {
    group = "distribution"
    description = "Build runtime image + MSI installer via jpackage"
    dependsOn("jpackage")
}