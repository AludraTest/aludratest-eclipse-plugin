package org.aludratest.eclipse.vde.internal.script;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "functionLibrary")
@XmlAccessorType(XmlAccessType.FIELD)
public class FunctionLibrary {

	@XmlElement(name = "category", type = ScriptCategory.class)
	private List<ScriptCategory> categories;

	public List<ScriptCategory> getCategories() {
		return categories;
	}

	private static FunctionLibrary load(InputStream in) throws IOException, JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(FunctionLibrary.class);
        Unmarshaller marshaller = ctx.createUnmarshaller();
        return (FunctionLibrary) marshaller.unmarshal(in);
	}

	public static FunctionLibrary load(Locale locale) throws IOException, JAXBException {
		String res = "functionLibrary_" + locale.getLanguage() + ".xml";

		InputStream in = FunctionLibrary.class.getResourceAsStream(res);
		if (in == null) {
			if (locale.getLanguage().equals("en")) {
				throw new FileNotFoundException(res);
			}
			return load(Locale.ENGLISH);
		}

		try {
			return load(in);
		}
		finally {
			try {
				in.close();
			}
			catch (Exception e) {
			}
		}
	}
}
