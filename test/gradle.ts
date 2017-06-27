import * as shell from "shelljs";
import * as child_process from "child_process";
import * as fs from "fs";
import { assert } from "chai";

describe("build.gradle", function() {
    this.timeout(20000);

    beforeEach("clean project", () => {
        shell.rm("-rf", "test/gradle/app1/platforms");
        shell.rm("-rf", "test/gradle/app1/node_modules");
        shell.mkdir("-p",
            "test/gradle/app1/platforms/android/libs/",
            "test/gradle/app1/platforms/android/src/main/assets/app/",
            "test/gradle/app1/platforms/android/src/main/res/drawable-nodpi/");
    });
    beforeEach("setup app artefacts", () => {
        child_process.spawnSync("npm", ["i"], { cwd: "test/gradle/app1" });
        shell.cp("-R", "test/gradle/app1/app/app.js", "test/gradle/app1/platforms/android/src/main/assets/app/app.js");
        shell.cp("-R", "test/gradle/app1/app/package.json", "test/gradle/app1/platforms/android/src/main/assets/app/package.json");
        shell.cp("-R", "test/gradle/app1/app/dependencies.json", "test/gradle/app1/platforms/android/dependencies.json");
        shell.cp("-R", "test/gradle/app1/app/icon.png", "test/gradle/app1/platforms/android/src/main/res/drawable-nodpi/icon.png");
    });
    beforeEach("copy runtime artefacts", () => {
        shell.cp("-R", "build-artifacts/project-template-gradle/*", "test/gradle/app1/platforms/android/");
        shell.cp("-R", "dist/framework/libs/*", "test/gradle/app1/platforms/android/libs/runtime-libs/");
        shell.cp("-R", "dist/framework/build-tools/*", "test/gradle/app1/platforms/android/build-tools/");
    });

    it("builds an app with some plugins with aars and optimized nativescript runtime", function() {
        build("app1");
        expectExpodedAar("nativescript-optimized");
        expectExpodedAar("awesomelib");
        expectExpodedAar("net.gotev/uploadservice");
        // TODO: Assert the nativescript-service AndroidManifest.xml has been merged.
        // E.g. the generated manifest has com.alexbbb.uploadservice.UploadService with the org.nativescript.imagepicker.uploadservice.action.upload intent service
    });
});

var projectPath;
function build(app) {
    projectPath = `test/gradle/${app}/platforms/android`;
    let proc = child_process.spawnSync("./gradlew", ["assembleDebug"], {
        cwd: projectPath,
        stdio: "inherit"
    });
    assert.equal(proc.status, 0, "Expected gradle build exit code to be 0");
}

function expectExpodedAar(name) {
    const intermediateAar = `${projectPath}/build/intermediates/exploded-aar/${name}`;
    assert(fs.existsSync(intermediateAar), `Expected the ${name} aar to be used in the intermediates: ${intermediateAar}`);
}
