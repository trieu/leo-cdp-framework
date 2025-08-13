package test.util;

import java.util.ArrayList;
import java.util.List;

import leotech.system.util.XssFilterUtil;

/**
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public class XssFilterTest {

    private static int passed = 0;
    private static int failed = 0;

    private static class TestCase {
        final String description;
        final Object input;
        final String expected;

        TestCase(String description, Object input, String expected) {
            this.description = description;
            this.input = input;
            this.expected = expected;
        }
    }

    public static void main(String[] args) {
        List<TestCase> tests = new ArrayList<>();

        tests.add(new TestCase("Basic script", "<div>Hello</div><script>alert('XSS');</script><p>World</p>", "<div>Hello</div><p>World</p>"));
        tests.add(new TestCase("Script with attributes", "<script type='text/javascript'>console.log('test');</script><h1>Title</h1>", "<h1>Title</h1>"));
        tests.add(new TestCase("Mixed case SCRIPT", "<SCRIPT>alert('XSS')</SCRIPT><span>Safe</span>", "<span>Safe</span>"));
        tests.add(new TestCase("Script with newlines", "<script>\nconsole.log('multi-line');\n</script><div>OK</div>", "<div>OK</div>"));
        tests.add(new TestCase("Multiple script blocks", "<script>one</script>middle<script>two</script>end", "middleend"));
        tests.add(new TestCase("Malformed script tag", "1<script>alert('xss')<div>Still here</div>", "1"));
        tests.add(new TestCase("Empty string", "", ""));
        tests.add(new TestCase("No script tag", "<b>Bold</b><i>Italic</i>", "<b>Bold</b><i>Italic</i>"));
        tests.add(new TestCase("Null input", null, ""));
        tests.add(new TestCase("Numeric input", 12345, "12345"));
        tests.add(new TestCase("Boolean input", true, "true"));
        tests.add(new TestCase("Script tag inside comment", "<!-- <script>alert('XSS')</script> --><div>Safe</div>", "<div>Safe</div>"));
        tests.add(new TestCase("pre json", "<pre>{json: true}</pre>", "<pre>{json: true}</pre>"));
        tests.add(new TestCase("form submit input", "<form action='/steal'><input type='submit'></form>", ""));
        tests.add(new TestCase("onclick doBadThings", "<div onclick=\"doBadThings()\">Click me</div>", "<div>Click me</div>"));
        tests.add(new TestCase("object tag load", "1<object data='bad.swf'></object>", "1"));
        
        tests.add(new TestCase("Image with an embedded script", "1<img src=\"x\" onerror=\"alert('XSS')\">", "1<img />"));
        tests.add(new TestCase("Injecting a malicious link", "<a href=\"javascript:alert('XSS')\">Click me</a><h1>1</h1>", "<a>Click me</a><h1>1</h1>"));
        tests.add(new TestCase("JSON data ", 
        		"{'email':'trieu@leocdp.com','html':'<div>Hello</div><script>alert('XSS');</script><p>World</p>','n':1,'b':true}", 
        		"{'email':'trieu@leocdp.com','html':'<div>Hello</div><p>World</p>','n':1,'b':true}"));
        tests.add(new TestCase("Injecting an iframe ", "<iframe src=\"http://attacker.com/malicious.html\"></iframe>", ""));
        
        
        for (TestCase test : tests) {
            runTest(test);
        }

        System.out.println("=====================================");
        System.out.printf("✅ Passed: %d / %d\n", passed, passed + failed);
        if (failed > 0) {
            System.out.printf("❌ Failed: %d\n", failed);
        }
        System.out.println("=====================================");
    }

    private static void runTest(TestCase test) {
        String result = XssFilterUtil.clean(test.input);
        boolean pass = result.equals(test.expected);

        if (pass) {
            passed++;
            System.out.printf("[PASS] %s\n", test.description);
        } else {
            failed++;
            System.out.printf("[FAIL] %s\n", test.description);
            System.out.println("  Input:    " + test.input);
            System.out.println("  Output:   " + result);
            System.out.println("  Expected: " + test.expected);
        }

        System.out.println();
    }
}
