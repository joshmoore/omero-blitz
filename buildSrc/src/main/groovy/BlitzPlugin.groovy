import dslplugin.DslPluginBase
import dslplugin.DslTask
import dslplugin.VelocityExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class BlitzPlugin implements Plugin<Project> {

    /**
     * Sets the group name for the DSLPlugin tasks to reside in.
     * i.e. In a terminal, call `./gradlew tasks` to list tasks in their groups in a terminal
     */
    static final def GROUP = 'omero'

    BlitzExtension blitzExt

    @Override
    void apply(Project project) {
        // Apply the base plugin, it's all we need
        project.plugins.apply(DslPluginBase)

        // Add the 'blitz' extension object
        blitzExt = project.extensions.create('blitz', BlitzExtension)

        // Default config for blitz
        blitzExt.mappingsDir = new File("${project.buildDir}/extracted")
        blitzExt.combinedDir = new File("${project.buildDir}/combined")

        configureImportMappingsTask(project)
        configureCombineTask(project)
        configureSplitTask(project)
    }

    def configureImportMappingsTask(Project project) {
        def task = project.task('importMappings', type: ImportMappingsTask) {
            group = GROUP
            description 'Extracts mapping files from dependency org.openmicroscopy:omero-model'
        }

        project.afterEvaluate {
            task.extractDir = blitzExt.mappingsDir
        }
    }

    def configureCombineTask(Project project) {
        def task = project.task('processCombine', type: DslTask) {
            group = GROUP
            description 'Processes the combined.vm'
        }

        // Config for velocity
        VelocityExtension ve = project.dsl.velocity
        ve.file_resource_loader_path = "buildSrc/src/main/resources"

        // Create properties
        Properties props = DslPluginBase.createVelocityProperties(ve)

        project.afterEvaluate {
            // Add task to process combine.vm
            // Configure velocity
            task.velocityProps = props
            task.template = new File("combined.vm")
            task.omeXmlFiles = project.fileTree(dir: blitzExt.mappingsDir, include: '**/*.ome.xml')
            task.outputPath = blitzExt.combinedDir
            task.formatOutput = { st -> "${st.getShortname()}I.combined" }
        }

        project.tasks.findByName('importMappings').dependsOn(task.name)
    }

    def configureSplitTask(Project project) {

        /*project.afterEvaluate {
            // Check only supported languages are used
            def langs = blitzExt.languages.collect() {
                def val = Language.find(it)
                if (val == null) {
                    throw new GradleException("Unsupported language")
                }
                return val
            }

            project.task("splitCombined", type: SplitTask) {
                group = GROUP
                languages = langs
                outputDir = blitzExt.outputDir
                combinedDir = blitzExt.combinedDir
            }
        }*/
    }
}

