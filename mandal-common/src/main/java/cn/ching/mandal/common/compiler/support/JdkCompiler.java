package cn.ching.mandal.common.compiler.support;

import cn.ching.mandal.common.utils.ClassHelper;
import com.sun.org.apache.bcel.internal.generic.RET;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * 2018/1/9
 * as JDK Compiler to compile code
 * @author chi.zhang
 * @email laxzhang@outlook.com
 */
public class JdkCompiler extends AbstractCompiler{

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    private DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector();

    private final JavaFileManagerImpl javaFileManager;

    private final ClassLoaderImpl classLoader;

    private volatile List<String> option;

    public JdkCompiler(){
        option = new ArrayList<>();
        option.add("-target");
        option.add("1.8");
        StandardJavaFileManager manager = compiler.getStandardFileManager(collector, null, null);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader instanceof URLClassLoader && (!loader.getClass().getName().equals("sun.misc.Launcher$AppClassLoader"))){
            try {
                URLClassLoader urlClassLoader = (URLClassLoader) loader;
                List<File> files = new ArrayList<>();
                for (URL url : urlClassLoader.getURLs()){
                    files.add(new File(url.getFile()));
                }
                manager.setLocation(StandardLocation.CLASS_PATH, files);
            } catch (IOException e){
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        classLoader = AccessController.doPrivileged((PrivilegedAction<ClassLoaderImpl>) () -> new ClassLoaderImpl(loader));
        javaFileManager = new JavaFileManagerImpl(manager, classLoader);
    }

    @Override
    protected Class<?> doCompile(String name, String source) throws Throwable {
        // get package name and class name
        int i = name.lastIndexOf(".");
        String pkg = i < 0 ? "" : name.substring(0, i);
        String cls = i < 0 ? "" : name.substring(i+1);
        JavaFileObject javaFileObject = new JavaFileObjectImpl(cls, source);
        javaFileManager.putFileForInput(StandardLocation.SOURCE_PATH, pkg, cls + ClassUtils.JAVA_EXTENSION, javaFileObject);
        // do compile task
        Boolean res = compiler.getTask(null, javaFileManager, collector, option, null, Arrays.asList(new JavaFileObject[]{javaFileObject})).call();
        if (res == null || !res.booleanValue()){
            throw new IllegalStateException("failed compile java source, class name is [" + name + "]" + "cause: [" + collector);
        }
        return classLoader.loadClass(name);
    }

    /**
     * make java source code to java source file
     */
    private static final class JavaFileObjectImpl extends SimpleJavaFileObject{

        private final CharSequence source;
        private ByteArrayOutputStream byteCode;

        public JavaFileObjectImpl(final String baseName, final CharSequence source){
            super(ClassUtils.toURI(baseName + ClassUtils.JAVA_EXTENSION), Kind.SOURCE);
            this.source = source;
        }

        JavaFileObjectImpl(final String name, final Kind kind){
            super(ClassUtils.toURI(name), kind);
            this.source = null;
        }

        /**
         * @param uri  the URI for this file object
         * @param kind the kind of this file object
         */
        public JavaFileObjectImpl(URI uri, Kind kind) {
            super(uri, kind);
            this.source = null;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            if (Objects.isNull(source)){
                throw new UnsupportedOperationException("source is null");
            }
            return source;
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return new ByteArrayInputStream(getByteCode());
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return byteCode = new ByteArrayOutputStream();
        }

        public byte[] getByteCode() {
            return byteCode.toByteArray();
        }
    }

    private static final class JavaFileManagerImpl extends ForwardingJavaFileManager<JavaFileManager>{

        private final ClassLoaderImpl classLoader;

        private final Map<URI, JavaFileObject> fileObjects = new HashMap<>();

        public JavaFileManagerImpl(JavaFileManager fileManager, ClassLoaderImpl classLoader) {
            super(fileManager);
            this.classLoader = classLoader;
        }

        private URI uri(Location location, String packageName, String relativeName){
            return ClassUtils.toURI(location.getName() + "/" + packageName + "/" + relativeName);
        }

        public void putFileForInput(StandardLocation location, String packageName, String relativeName, JavaFileObject file){
            fileObjects.put(uri(location, packageName, relativeName), file);
        }

        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
            FileObject o = fileObjects.get(uri(location, packageName, relativeName));
            if (Objects.nonNull(o)){
                return o;
            }
            return super.getFileForInput(location, packageName, relativeName);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject outputFile) throws IOException {
            JavaFileObjectImpl file = new JavaFileObjectImpl(className, kind);
            classLoader.add(className, file);
            return file;
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
            return classLoader;
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            if (file instanceof JavaFileObjectImpl){
                return file.getName();
            }

            return super.inferBinaryName(location, file);
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {

            Iterable<JavaFileObject> result = super.list(location, packageName, kinds, recurse);

            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            List<URL> urls = new ArrayList<>();
            Enumeration<URL> e = contextClassLoader.getResources("cn");
            while (e.hasMoreElements()){
                urls.add(e.nextElement());
            }

            ArrayList<JavaFileObject> files = new ArrayList<>();
            if (location == StandardLocation.CLASS_PATH && kinds.contains(JavaFileObject.Kind.CLASS)){
                files.stream().filter(o -> o.getKind() == JavaFileObject.Kind.CLASS && o.getName().startsWith(packageName)).forEach(o -> files.add(o));
            } else if (location == StandardLocation.SOURCE_PATH && kinds.contains(JavaFileObject.Kind.SOURCE)){
                files.stream().filter(o -> o.getKind() == JavaFileObject.Kind.SOURCE && o.getName().startsWith(packageName)).forEach(o -> files.add(o));
            }

            for (JavaFileObject object : result){
                files.add(object);
            }

            return files;
        }
    }

    private static final class ClassLoaderImpl extends ClassLoader{

        private final Map<String, JavaFileObject> classes = new HashMap<>();

        ClassLoaderImpl(final ClassLoader parentClassLoader){
            super((parentClassLoader));
        }

        Collection<JavaFileObject> files(){
            return Collections.unmodifiableCollection(classes.values());
        }

        void add(final String qualifiedClassName, final JavaFileObject javaFile){
            classes.put(qualifiedClassName, javaFile);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            JavaFileObject javaFileObject = classes.get(name);
            if (javaFileObject != null){
                byte[] bytes = ((JavaFileObjectImpl) javaFileObject).getByteCode();
                return defineClass(name, bytes, 0, bytes.length);
            }
            try {
                return ClassHelper.forNameWithCallerClassLoader(name, getClass());
            }catch (ClassNotFoundException e){
                return super.findClass(name);
            }
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (name.endsWith(ClassUtils.CLASS_EXTENSION)){
                String qualifiedClassName = name.substring(0, ClassUtils.CLASS_EXTENSION.length()).replace("/", ".");
                JavaFileObjectImpl file = (JavaFileObjectImpl) classes.get(qualifiedClassName);
                if (Objects.nonNull(file)){
                    return new ByteArrayInputStream(file.getByteCode());
                }
            }
            return super.getResourceAsStream(name);
        }
    }
}
