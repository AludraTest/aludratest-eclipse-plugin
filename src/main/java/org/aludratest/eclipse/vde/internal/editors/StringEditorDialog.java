package org.aludratest.eclipse.vde.internal.editors;

import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aludratest.eclipse.vde.internal.VdeImage;
import org.aludratest.eclipse.vde.internal.contentassist.JavaScriptContentProposalProvider;
import org.aludratest.eclipse.vde.internal.model.AbstractModelNode;
import org.aludratest.eclipse.vde.internal.script.ScriptFunction;
import org.aludratest.eclipse.vde.model.IFieldValue;
import org.aludratest.eclipse.vde.model.IStringValue;
import org.aludratest.eclipse.vde.model.ITestDataConfigurationSegment;
import org.aludratest.eclipse.vde.model.ITestDataFieldMetadata;
import org.aludratest.eclipse.vde.model.ITestDataFieldValue;
import org.aludratest.eclipse.vde.model.ITestDataMetadata;
import org.aludratest.eclipse.vde.model.TestDataFieldType;
import org.aludratest.exception.AutomationException;
import org.aludratest.testcase.data.impl.xml.DefaultScriptLibrary;
import org.aludratest.testcase.data.impl.xml.ScriptLibrary;
import org.aludratest.testcase.data.impl.xml.XmlBasedTestDataProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class StringEditorDialog extends TitleAreaDialog {

	private Control plainValueControl;

	private ITestDataFieldValue fieldValue;

	private ITestDataFieldMetadata fieldMetadata;

	private Button btnPlainValue;

	private Button btnScript;

	private Button btnFunction;

	private Text txtScript;

	private Label lblPreview;

	private static final Pattern PATTERN_DATE = Pattern.compile("([1-9][0-9]{3})-(0[1-9]|1[0-2])-([0-3][0-9])");

	private static final SimpleDateFormat FORMAT_DATE = new SimpleDateFormat("yyyy-MM-dd");

	private XmlBasedTestDataProvider xmlProvider;

	private Map<String, Object> precalcOtherFields;

	public StringEditorDialog(Shell parentShell, ITestDataFieldValue fieldValue, ITestDataFieldMetadata fieldMetadata) {
		super(parentShell);
		this.fieldValue = fieldValue;
		this.fieldMetadata = fieldMetadata;
		if (fieldValue.getFieldValue().getValueType() != IFieldValue.TYPE_STRING) {
			throw new IllegalArgumentException("Unsupported field value for string editor: "
					+ fieldValue.getFieldValue().getClass().getName());
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite client = (Composite) super.createDialogArea(parent);
		Composite c = new Composite(client, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		c.setLayout(new GridLayout(1, false));

		// "plain value" section
		btnPlainValue = new Button(c, SWT.RADIO);
		btnPlainValue.setText("Plain value:");

		plainValueControl = createPlainValueControl(c);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 16 + FieldDecorationRegistry.getDefault().getMaximumDecorationWidth();
		gd.widthHint = 300;
		plainValueControl.setLayoutData(gd);

		btnScript = new Button(c, SWT.RADIO);
		btnScript.setText("Script result:");
		gd = new GridData(SWT.LEFT, SWT.TOP, false, false);
		gd.verticalIndent = 8;
		btnScript.setLayoutData(gd);

		Composite cpo = new Composite(c, SWT.NONE);
		cpo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		cpo.setLayout(new GridLayout(2, false));
		txtScript = new Text(cpo, SWT.BORDER | SWT.LEFT);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = 16 + FieldDecorationRegistry.getDefault().getMaximumDecorationWidth();
		gd.widthHint = 300;
		txtScript.setLayoutData(gd);

		FieldDecoration fdec = FieldDecorationRegistry.getDefault().getFieldDecoration(
				FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		ControlDecoration cdec = new ControlDecoration(txtScript, SWT.TOP | SWT.LEFT);
		cdec.setImage(fdec.getImage());
		cdec.setDescriptionText("Any ECMAScript expression is supported. Strg+Space for list of available fields; click script button for list of available functions.");
		cdec.setShowOnlyOnFocus(false);
		JavaScriptContentProposalProvider.attachToComponent(txtScript, jsVarsSource);

		btnFunction = new Button(cpo, SWT.PUSH);
		btnFunction.setImage(VdeImage.FUNCTION.getImage());
		btnFunction.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openFunctionDialog();
			}
		});

		Label lbl = new Label(c, SWT.LEFT);
		lbl.setText("Preview: ");
		if (fieldMetadata.getFormatterPattern() != null) {
			lbl = new Label(c, SWT.LEFT);
			lbl.setText("  Active format pattern: " + fieldMetadata.getFormatterPattern());
		}

		Composite c1 = new Composite(c, SWT.NONE);
		FillLayout fl = new FillLayout();
		fl.marginWidth = 25;
		c1.setLayout(fl);
		c1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		cpo = new Composite(c1, SWT.BORDER);
		gd = new GridData(SWT.CENTER, SWT.END, true, false);
		gd.widthHint = 240;
		gd.heightHint = 60;
		fl = new FillLayout();
		fl.marginHeight = 20;
		cpo.setLayout(fl);

		lblPreview = new Label(cpo, SWT.CENTER);
		lblPreview.setText("This could be YOUR preview.");

		SelectionAdapter selAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateControlsEnabled();
				updatePreview();
			}
		};
		btnPlainValue.addSelectionListener(selAdapter);
		btnScript.addSelectionListener(selAdapter);

		btnPlainValue.setSelection(!fieldValue.isScript());
		btnScript.setSelection(fieldValue.isScript());

		txtScript.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updatePreview();
			}
		});

		String val = ((IStringValue) fieldValue.getFieldValue()).getValue();
		if (!fieldValue.isScript()) {
			setValueToPlainValueControl(val == null ? "" : val);
		}
		else {
			txtScript.setText(val);
		}

		updatePreview();
		updateControlsEnabled();

		getShell().setText("Edit field value");
		setTitle("Edit field value");
		setMessage("Set the value for this field. Either enter or select a plain value, or use a script for dynamic field value calculation.");

		return client;
	}

	private Callable<Set<String>> jsVarsSource = new Callable<Set<String>>() {
		@Override
		public Set<String> call() {
			if (precalcOtherFields == null) {
				calculateOtherFields();
			}

			return precalcOtherFields.keySet();
		}
	};

	private void updateControlsEnabled() {
		if (plainValueControl != null && !plainValueControl.isDisposed()) {
			plainValueControl.setEnabled(btnPlainValue.getSelection());
		}

		txtScript.setEnabled(btnScript.getSelection());
		btnFunction.setEnabled(btnScript.getSelection());
	}

	private void updatePreview() {
		if (btnPlainValue.getSelection()) {
			lblPreview.setText(getFormattedPlainValueFromControl());
			if (getButton(OK) != null) {
				getButton(OK).setEnabled(true);
			}
		}
		else {
			if (txtScript == null) {
				return;
			}
			// calculate script value; format
			String script = txtScript.getText();
			if (xmlProvider == null) {
				xmlProvider = createTestDataProvider();
			}

			try {
				if (precalcOtherFields == null) {
					calculateOtherFields();
				}
				String evalResult = xmlProvider.evaluate(script, fieldMetadata.getFormatterPattern(),
						toLocale(fieldMetadata.getFormatterLocale()), precalcOtherFields);
				lblPreview.setText(evalResult == null ? "" : evalResult);
				if (getButton(OK) != null) {
					getButton(OK).setEnabled(true);
				}
			}
			catch (AutomationException e) {
				lblPreview.setText("SCRIPT ERROR: " + e.getCause().getMessage());
				if (getButton(OK) != null) {
					getButton(OK).setEnabled(false);
				}
			}
		}
	}

	private void openFunctionDialog() {
		ScriptFunctionDialog dlg = new ScriptFunctionDialog(getShell());
		if (dlg.open() == ScriptFunctionDialog.OK) {
			ScriptFunction fn = dlg.getSelectedFunction();
			if (fn != null) {
				txtScript.insert(fn.getScriptToInsert());
			}
		}
	}

	@Override
	protected void okPressed() {
		if (btnPlainValue.getSelection()) {
			// transfer plain value to field
			String plainValue = getPlainValueFromControl();
			((IStringValue) fieldValue.getFieldValue()).setValue(plainValue);
			fieldValue.setScript(false);
		}
		else if (btnScript.getSelection()) {
			fieldValue.setScript(true);
			((IStringValue) fieldValue.getFieldValue()).setValue(txtScript.getText());
		}

		super.okPressed();
	}

	private Control createPlainValueControl(Composite parent) {
		TestDataFieldType fieldType = fieldMetadata.getType();
		if (fieldType == TestDataFieldType.STRING || fieldType == TestDataFieldType.NUMBER) {
			Text txt = new Text(parent, SWT.BORDER | SWT.LEFT);
			txt.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					updatePreview();
				}
			});
			return txt;
		}
		if (fieldType == TestDataFieldType.BOOLEAN) {
			Button btnPlainValue = new Button(parent, SWT.CHECK);
			btnPlainValue.setText("Check for a value of \"true\"");
			btnPlainValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updatePreview();
				}
			});
			return btnPlainValue;
		}
		if (fieldType == TestDataFieldType.DATE) {
			DateTime dt = new DateTime(parent, SWT.CALENDAR | SWT.DROP_DOWN);
			dt.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updatePreview();
				}
			});
			return dt;
		}

		throw new IllegalArgumentException("Unsupported type for StringEditorDialog: " + fieldType);
	}

	private void setValueToPlainValueControl(String value) {
		TestDataFieldType fieldType = fieldMetadata.getType();
		if (fieldType == TestDataFieldType.STRING || fieldType == TestDataFieldType.NUMBER) {
			Text txtPlainValue = (Text) plainValueControl;
			txtPlainValue.setText(value == null ? "" : value);
		}
		if (fieldType == TestDataFieldType.BOOLEAN) {
			Button btnPlainValue = (Button) plainValueControl;
			btnPlainValue.setSelection("true".equals(value));
		}
		if (fieldType == TestDataFieldType.DATE) {
			DateTime dtPlainValue = (DateTime) plainValueControl;
			if (value != null) {
				Matcher m = PATTERN_DATE.matcher(value);
				if (m.matches()) {
					dtPlainValue
							.setDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
				}
			}
		}
	}

	private String getPlainValueFromControl() {
		TestDataFieldType fieldType = fieldMetadata.getType();
		if (fieldType == TestDataFieldType.STRING || fieldType == TestDataFieldType.NUMBER) {
			Text txtPlainValue = (Text) plainValueControl;
			return txtPlainValue.getText();
		}
		if (fieldType == TestDataFieldType.BOOLEAN) {
			Button btnPlainValue = (Button) plainValueControl;
			return btnPlainValue.getSelection() ? "true" : "false";
		}
		if (fieldType == TestDataFieldType.DATE) {
			DateTime dtPlainValue = (DateTime) plainValueControl;
			return dtPlainValue.getYear() + "-" + toTwoDigits(dtPlainValue.getMonth() + 1) + "-" + dtPlainValue.getDay();
		}

		return null;
	}

	private Date getDateFromPlainValueControl() {
		try {
			return FORMAT_DATE.parse(getPlainValueFromControl());
		}
		catch (ParseException e) {
			return null;
		}
	}

	private String getFormattedPlainValueFromControl() {
		TestDataFieldType fieldType = fieldMetadata.getType();
		if (fieldType == TestDataFieldType.STRING || fieldType == TestDataFieldType.NUMBER) {
			Text txtPlainValue = (Text) plainValueControl;
			if (fieldType == TestDataFieldType.STRING) {
				return txtPlainValue.getText();
			}

			String pattern = fieldMetadata.getFormatterPattern();
			if (pattern == null) {
				pattern = "#.#";
			}

			try {
				DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
				return format.format(Double.parseDouble(txtPlainValue.getText()));
			}
			catch (NumberFormatException e) {
				return txtPlainValue.getText();
			}
			catch (IllegalArgumentException e) {
				return txtPlainValue.getText();
			}
		}
		if (fieldType == TestDataFieldType.BOOLEAN) {
			return getPlainValueFromControl();
		}
		if (fieldType == TestDataFieldType.DATE) {
			Date dt = getDateFromPlainValueControl();
			if (dt == null) {
				return getPlainValueFromControl();
			}
			String pattern = fieldMetadata.getFormatterPattern();
			if (pattern == null) {
				pattern = "yyyy-MM-dd";
			}

			return new SimpleDateFormat(pattern, Locale.US).format(dt);
		}

		return null;
	}

	private String toTwoDigits(int value) {
		if (value < 10) {
			return "0" + value;
		}
		return "" + value;
	}

	private XmlBasedTestDataProvider createTestDataProvider() {
		XmlBasedTestDataProvider provider = new XmlBasedTestDataProvider();

		Map<String, ScriptLibrary> libs = new HashMap<String, ScriptLibrary>();
		libs.put("default", new DefaultScriptLibrary());

		setVariableValueInObject(provider, "scriptLibraries", libs);
		return provider;
	}

	private void calculateOtherFields() {
		ITestDataMetadata globalMetadata = null;
		if (fieldMetadata instanceof AbstractModelNode) {
			globalMetadata = (ITestDataMetadata) ((AbstractModelNode) fieldMetadata).getParentNode().getParentNode();
		}

		precalcOtherFields = new HashMap<String, Object>();

		if (fieldValue instanceof AbstractModelNode) {
			ITestDataConfigurationSegment configSegment = (ITestDataConfigurationSegment) ((AbstractModelNode) fieldValue)
					.getParentNode();
			Map<String, ITestDataFieldValue> scriptFields = new HashMap<String, ITestDataFieldValue>();

			for (ITestDataFieldValue fv : configSegment.getFieldValues()) {
				if (fv.equals(fieldValue)) {
					continue;
				}
				if (fv.isScript()) {
					scriptFields.put(fv.getFieldName(), fv);
				}
				else if (fv.getFieldValue() != null && fv.getFieldValue().getValueType() == IFieldValue.TYPE_STRING) {
					precalcOtherFields.put(fv.getFieldName(), ((IStringValue) fv.getFieldValue()).getValue());
				}
			}

			int lastErrorCount;
			int errorCount = 0;

			do {
				lastErrorCount = errorCount;
				errorCount = 0;
				Iterator<Map.Entry<String, ITestDataFieldValue>> scriptIter = scriptFields.entrySet().iterator();
				while (scriptIter.hasNext()) {
					Map.Entry<String, ITestDataFieldValue> scriptField = scriptIter.next();
					IFieldValue fv = scriptField.getValue().getFieldValue();
					if (fv != null && fv.getValueType() == IFieldValue.TYPE_STRING) {
						// get metadata for field to determine formatter and locale
						String formatter = null;
						Locale locale = null;
						if (globalMetadata != null) {
							ITestDataFieldMetadata meta = scriptField.getValue().getMetadata(globalMetadata);
							if (meta != null) {
								formatter = meta.getFormatterPattern();
								locale = toLocale(meta.getFormatterLocale());
							}
						}

						try {
							String value = xmlProvider.evaluate(((IStringValue) fv).getValue(), formatter, locale,
									precalcOtherFields);
							precalcOtherFields.put(scriptField.getKey(), value);
							scriptIter.remove();
						}
						catch (AutomationException e) {
							errorCount++;
						}
					}
				}
			}
			while (!scriptFields.isEmpty() && errorCount != lastErrorCount);
		}
	}

	private static void setVariableValueInObject(Object object, String fieldName, Object value) {
		try {
			Class<?> clazz = object.getClass();
			Field f = clazz.getDeclaredField(fieldName);
			f.setAccessible(true);
			f.set(object, value);
			f.setAccessible(false);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Locale toLocale(String s) {
		if (s == null || "".equals(s)) {
			return null;
		}

		// language only?
		if (s.matches("[a-z]{2}")) {
			return new Locale(s);
		}

		// language and country?
		if (s.matches("[a-z]{2}_[A-Z]{2}")) {
			String[] parts = s.split("_");
			return new Locale(parts[0], parts[1]);
		}

		// variant?
		if (s.matches("[a-z]{2}_[A-Z]{2}_[^_]+")) {
			String[] parts = s.split("_");
			return new Locale(parts[0], parts[1], parts[2]);
		}

		// will fall back to default (Locale.US) in evaluation
		return null;
	}

}
