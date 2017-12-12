module.exports = function(grunt) {
    
      grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        clean: ['target/dist'],
        copy: {
          main: {
            expand: true,
            cwd: 'src/main/webapp',
            src: '**',
            dest: 'target/dist',
          },
        },
        cacheBust: {
            taskName: {
                options: {
                    assets: ['resources/js/**/*', 'resources/css/**/*'],
                    baseDir: 'target/dist/',
                    outputDir: 'resources/assets/',
                    clearOutputDir: true,
                    deleteOriginals: true,
                    queryString: false
                },
                files: [{
                  src: ['target/dist/index.html']
                }]
            }
        },
        ngtemplates:  {
          rcApp: {
            cwd:      'target/dist',
            src:      'modules/**/*.html',
            dest:     'target/dist/resources/js/restcomm.js',
            options:    {
              append: true
            }
          }
        },
      });
    
      grunt.loadNpmTasks('grunt-angular-templates');
      grunt.loadNpmTasks('grunt-contrib-clean');
      grunt.loadNpmTasks('grunt-contrib-copy');
      grunt.loadNpmTasks('grunt-cache-bust');
      
      grunt.registerTask('default', ['clean', 'copy', 'ngtemplates', 'cacheBust']);
    
    };
    
    