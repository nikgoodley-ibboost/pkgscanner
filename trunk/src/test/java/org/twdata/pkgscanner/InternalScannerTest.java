package org.twdata.pkgscanner;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.twdata.pkgscanner.pattern.PatternFactory;
import org.twdata.pkgscanner.pattern.CompiledPattern;

public class InternalScannerTest extends TestCase {
    private File tmpDir;

    @Override
    public void setUp() throws IOException {
        File tmp = File.createTempFile("foo", "bar");
        tmpDir = new File(tmp.getParentFile(), "footest");
        tmpDir.mkdir();
    }

    @Override
    public void tearDown() throws Exception {
        tmpDir.delete();
    }
    public void testDeterminePackageVersion() throws Exception {
        InternalScanner scanner = new InternalScanner(getClass().getClassLoader(), new PackageScanner.VersionMapping[] {});
        scanner.setOsgiVersionConverter(new OsgiVersionConverter() {
            public String getVersion(String version) { return version; }
        });
        assertEquals("2.0", scanner.determinePackageVersion(new File(tmpDir, "foo-2.0.jar"), "testpackage"));
        assertEquals("2.0_something", scanner.determinePackageVersion(new File(tmpDir, "foo-2.0_something.jar"), "testpackage"));
        assertEquals("2.0-beta", scanner.determinePackageVersion(new File(tmpDir, "foo-2.0-beta.jar"), "testpackage"));
        assertEquals("2", scanner.determinePackageVersion(new File(tmpDir, "foo-2.jar"), "testpackage"));
        assertEquals("2", scanner.determinePackageVersion(new File(tmpDir, "foo4-2.jar"), "testpackage"));
        assertEquals("1.2.8", scanner.determinePackageVersion(new File(tmpDir, "log4j-1.2.8.jar"), "testpackage"));
        assertEquals("2.0+xmlrpc61", scanner.determinePackageVersion(new File(tmpDir, "xmlrpc-2.0+xmlrpc61.jar"), "testpackage"));
        assertEquals(null, scanner.determinePackageVersion(new File(tmpDir, "foo-alpha.jar"), "testpackage"));
    }

    public void testDeterminePackageVersionWithExplicitVersion() throws Exception {
        PackageScanner.VersionMapping mapping = new PackageScanner.VersionMapping("testpackage").toVersion("34");
        mapping.setPatternFactory(new PatternFactory() {
            public CompiledPattern compile(final String pattern) {
                return new CompiledPattern() {
                    public String getOriginal() {
                        return pattern;
                    }

                    public boolean matches(String value) {
                        return value.equals(pattern);
                    }
                };
            }
        });
        InternalScanner scanner = new InternalScanner(getClass().getClassLoader(), new PackageScanner.VersionMapping[] {mapping});

        assertEquals("34", scanner.determinePackageVersion(new File(tmpDir, "foo-2.0.jar"), "testpackage"));
    }

    public void testLoadImplementationsInDirectory() throws Exception {
        File parent = new File(tmpDir, "parent");
        File child = new File(parent, "child");
        child.mkdirs();
        File baby = new File(child, "foo.class");
        baby.createNewFile();
        File baby2 = new File(child, "bfoo");
        baby2.createNewFile();

        InternalScanner scanner = new InternalScanner(getClass().getClassLoader(), new PackageScanner.VersionMapping[] {});
        Collection<ExportPackage> exports = scanner.loadImplementationsInDirectory(new InternalScanner.Test() {
            public boolean matchesPackage(String pkg) { return true; }
            public boolean matchesJar(String name) { return true; }
        }, "parent", parent);
        assertNotNull(exports);
        assertEquals(1, exports.size());
        assertEquals("parent.child", exports.iterator().next().getPackageName());
    }

    public void testFindInPackageWithPlusInFilename() throws Exception {

        URLClassLoader cl = new URLClassLoader(new URL[] {getClass().getResource("/foo+bar.jar")});
        InternalScanner scanner = new InternalScanner(cl, new PackageScanner.VersionMapping[] {});
        Collection<ExportPackage> exports = scanner.findInPackage(new InternalScanner.Test() {
            public boolean matchesPackage(String pkg) { return true; }
            public boolean matchesJar(String name) { return true; }
        }, "foo");
        assertNotNull(exports);
        assertEquals(1, exports.size());
        assertEquals("foo", exports.iterator().next().getPackageName());
    }
}
