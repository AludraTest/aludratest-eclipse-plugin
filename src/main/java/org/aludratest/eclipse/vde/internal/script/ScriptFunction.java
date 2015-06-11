package org.aludratest.eclipse.vde.internal.script;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class ScriptFunction {

	@XmlAttribute(name = "name", required = true)
	private String name;

	@XmlAttribute(name = "description", required = true)
	private String description;

	@XmlAttribute(name = "scriptToInsert", required = true)
	private String scriptToInsert;

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getScriptToInsert() {
		return scriptToInsert;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}

}
