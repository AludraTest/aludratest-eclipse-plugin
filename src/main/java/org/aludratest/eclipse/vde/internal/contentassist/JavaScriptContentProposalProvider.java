package org.aludratest.eclipse.vde.internal.contentassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

import org.aludratest.eclipse.vde.internal.VdeImage;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public class JavaScriptContentProposalProvider extends LabelProvider implements IContentProposalProvider {

	private Callable<Set<String>> objectsSource;

	public JavaScriptContentProposalProvider(Callable<Set<String>> objectsSource) {
		if (objectsSource == null) {
			throw new IllegalArgumentException("objectsSource is null");
		}
		this.objectsSource = objectsSource;
	}

	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		// get current word
		int start = 0, end = contents.length();
		for (int c = position - 1; c >= 0; c--) {
			if (isNonWordChar(contents.charAt(c))) {
				start = c + 1;
				break;
			}
		}
		for (int c = position; c < contents.length(); c++) {
			if (isNonWordChar(contents.charAt(c))) {
				end = c;
				break;
			}
		}

		String word = contents.substring(start, end).trim().toLowerCase(Locale.US);

		Set<String> allWords;
		try {
			allWords = objectsSource.call();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		if ("".equals(word)) {
			return buildProposals(allWords, contents, start, end);
		}

		// collect all words starting with word
		Set<String> matchingWords = new HashSet<String>();
		
		for (String w : allWords) {
			if (w.toLowerCase(Locale.US).startsWith(word)) {
				matchingWords.add(w);
			}
		}
		
		return buildProposals(matchingWords, contents, start, end);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ContentProposal) {
			ContentProposal cp = (ContentProposal) element;
			return cp.getLabel();
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		return VdeImage.LOCAL_VAR.getImage();
	}

	private boolean isNonWordChar(char ch) {
		return !Character.isLetterOrDigit(ch);
	}

	private IContentProposal[] buildProposals(Set<String> words, String contents, int startReplace, int endReplace) {
		List<String> sortedWords = new ArrayList<String>(words);
		Collections.sort(sortedWords, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		});

		List<IContentProposal> result = new ArrayList<IContentProposal>();
		for (String word : sortedWords) {
			String newContents = contents.substring(0, startReplace) + word + contents.substring(endReplace);
			int targetPos = startReplace + word.length();
			result.add(new ContentProposal(newContents, word, word, targetPos));
		}

		return result.toArray(new IContentProposal[0]);
	}

	public static void attachToComponent(Text txtScript, Callable<Set<String>> wordSource) {
		JavaScriptContentProposalProvider provider = new JavaScriptContentProposalProvider(wordSource);
		
		KeyStroke stroke;
		try {
			stroke = KeyStroke.getInstance("Ctrl+Space");
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}

		ContentProposalAdapter adapter = new ContentProposalAdapter(txtScript, new TextContentAdapter() {
			@Override
			public void insertControlContents(Control control, String text, int cursorPosition) {
				((Text) control).setText(text);
				((Text) control).setSelection(cursorPosition, cursorPosition);
			}
		}, provider, stroke, null);
		
		adapter.setLabelProvider(provider);
		adapter.setPopupSize(new Point(350, 200));
	}
}
