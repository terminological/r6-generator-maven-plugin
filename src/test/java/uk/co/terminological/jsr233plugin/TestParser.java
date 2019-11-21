package uk.co.terminological.jsr233plugin;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

public class TestParser {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public final void test() {
		System.out.println(Object[].class.getCanonicalName());
		System.out.println(Object[].class.getComponentType().getCanonicalName());
		ArrayList<String> test = new ArrayList<String>();
		System.out.println(test.getClass().getCanonicalName());
		System.out.println(test.getClass().getTypeParameters()[0].getGenericDeclaration());
	}

}
