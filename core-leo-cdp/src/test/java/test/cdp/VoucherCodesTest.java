package test.cdp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import leotech.system.util.RamdomCodesUtil;
import leotech.system.util.RamdomCodesUtil.Charset;
import leotech.system.util.RamdomCodesUtil.RandomCodeConfig;

public class VoucherCodesTest {

    @Test
    public void shouldGenerateCodeOfGivenLength() {
        // given
        RandomCodeConfig config = RandomCodeConfig.length(10);
        
        // when
        String code = RamdomCodesUtil.generate(config);
        
        // then
        assertThat(code.length()).isEqualTo(10);
    }
    
    @Test
    public void shouldGenerateNumericCode() {
        // given
        RandomCodeConfig config = RandomCodeConfig.length(8).withCharset(Charset.NUMBERS);
        
        // when
        String code = RamdomCodesUtil.generate(config);
        
        // then
        assertThat(code).matches("^([0-9]){8}$");
    }
    
    @Test
    public void shouldGenerateCodeWithPrefix() {
        // given
        RandomCodeConfig config = RandomCodeConfig.length(8).withPrefix("TEST-");
        
        // when
        String code = RamdomCodesUtil.generate(config);
        
        // then
        assertThat(code).startsWith("TEST-");
        assertThat(code.length()).isEqualTo(5 /*TEST-*/ + 8 /*random*/);
    }
    
    @Test
    public void shouldGenerateCodeWithPostfix() {
        // given
        RandomCodeConfig config = RandomCodeConfig.length(8).withPostfix("-TEST");
        
        // when
        String code = RamdomCodesUtil.generate(config);
        
        // then
        assertThat(code).endsWith("-TEST");
        assertThat(code.length()).isEqualTo(8 /*random*/ + 5 /*-TEST*/);
    }
    
    @Test
    public void shouldGenerateCodeWithPrefixAndPostfix() {
        // given
        RandomCodeConfig config = RandomCodeConfig.length(8).withPrefix("TE-").withPostfix("-ST");
        
        // when
        String code = RamdomCodesUtil.generate(config);
        
        System.out.println(code);
        
        // then
        assertThat(code).startsWith("TE-");
        assertThat(code).endsWith("-ST");
        assertThat(code.length()).isEqualTo(3 /*TE-*/ + 8 /*random*/ + 3 /*-ST*/);
    }
    
    @Test
    public void shouldGenerateCodeFromGivenPattern() {
        // given
        RandomCodeConfig config = RandomCodeConfig.pattern("##-###-##");
        
        // when
        String code = RamdomCodesUtil.generate(config);
        
        // then
        assertThat(code).matches("^([0-9a-zA-Z]){2}-([0-9a-zA-Z]){3}-([0-9a-zA-Z]){2}$");
    }
    
    public static void main(String[] args) {
    	 RandomCodeConfig config = RandomCodeConfig.length(10).withCharset(Charset.NUMBERS);
         
         // when
         String code = RamdomCodesUtil.generate(config);
         
         System.out.println(code);
	}
}
