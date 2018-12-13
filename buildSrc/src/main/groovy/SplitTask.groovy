import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class SplitSpec {
    Language language
    Prefix prefix
    String output
}

class SplitTask extends DefaultTask {

    /**
     * List of the languages we want to split from .combined files
     */
    @Input
    String language

    /**
     * Directory to spit out source files
     */
    @OutputDirectory
    File outputDir

    String outputName

    /**
     * Directory to .combined files
     */
    @InputDirectory
    File combinedDir

    @TaskAction
    void action() {
        // Lookup the language
        Language lang = Language.find(language)
        if (lang == null) {
            throw new GradleException("Unsupported language : ${language}")
        }

        def myCopySpec = project.copySpec {
            from(combinedDir) {
                include '**/*.combined'
            }
        }

        lang.prefixes.each { prefix ->
            final def prefixName = prefix.name().toLowerCase()
            project.copy {
                into "${outputDir}/${prefixName}"
                with myCopySpec
                filter { filerLine(it, prefixName) }
                rename '(.*?)I[.]combined', "omero/model/\$1I.${prefix.extension}"
            }
        }
    }

    static def filerLine(String line, String prefix) {
        return line.matches("^\\[all](.*)|^\\[${prefix}](.*)") ?
                line.replaceAll("^\\[all]|^\\[${prefix}]", "") :
                null
    }
}
