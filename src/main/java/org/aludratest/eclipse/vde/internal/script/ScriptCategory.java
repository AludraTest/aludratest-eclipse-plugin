package org.aludratest.eclipse.vde.internal.script;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class ScriptCategory {

	@XmlAttribute(name = "name", required = true)
	private String name;

	@XmlElement(name = "function", type = ScriptFunction.class)
	private List<ScriptFunction> functions;

	public String getName() {
		return name;
	}

	public List<ScriptFunction> getFunctions() {
		return functions;
	}

}
