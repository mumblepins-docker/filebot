
package net.sourceforge.filebot.ui.panel.rename;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

import javax.swing.AbstractAction;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.tuned.ExceptionUtilities;
import net.sourceforge.tuned.FileUtilities;


class RenameAction extends AbstractAction {
	
	private final RenameModel model;
	
	
	public RenameAction(RenameModel model) {
		super("Rename", ResourceManager.getIcon("action.rename"));
		
		putValue(SHORT_DESCRIPTION, "Rename files");
		
		this.model = model;
	}
	

	public void actionPerformed(ActionEvent evt) {
		Deque<Match<File, File>> todoQueue = new ArrayDeque<Match<File, File>>();
		Deque<Match<File, File>> doneQueue = new ArrayDeque<Match<File, File>>();
		
		for (Match<String, File> match : model.getMatchesForRenaming()) {
			File source = match.getCandidate();
			String extension = FileUtilities.getExtension(source);
			
			StringBuilder name = new StringBuilder(match.getValue());
			
			if (extension != null) {
				name.append(".").append(extension);
			}
			
			// same parent, different name
			File target = new File(source.getParentFile(), name.toString());
			
			todoQueue.addLast(new Match<File, File>(source, target));
		}
		
		try {
			int renameCount = todoQueue.size();
			
			for (Match<File, File> match : todoQueue) {
				// rename file
				if (!match.getValue().renameTo(match.getCandidate()))
					throw new IOException(String.format("Failed to rename file: %s.", match.getValue().getName()));
				
				// revert in reverse order if renaming of all matches fails
				doneQueue.addFirst(match);
			}
			
			// renamed all matches successfully
			Logger.getLogger("ui").info(String.format("%d files renamed.", renameCount));
		} catch (IOException e) {
			// rename failed
			Logger.getLogger("ui").warning(ExceptionUtilities.getRootCauseMessage(e));
			
			boolean revertSuccess = true;
			
			// revert rename operations
			for (Match<File, File> match : doneQueue) {
				revertSuccess &= match.getCandidate().renameTo(match.getValue());
			}
			
			if (!revertSuccess) {
				Logger.getLogger("ui").severe("Failed to revert all rename operations.");
			}
		}
		
	}
}
