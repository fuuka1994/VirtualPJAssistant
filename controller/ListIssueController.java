package controller;

import helper.DatabaseHelper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;

import model.Developer;
import model.Employee;
import model.Issue;
import model.Issue.ISSUE_STATUS;
import model.Manager;
import model.Project;
import view.IssueDetail;
import view.IssueHistory;
import view.ListIssue;

public class ListIssueController {
	private ListIssue listIssueView;

	public ListIssueController(Project currentProject, Employee currentEmployee) {
		listIssueView = new ListIssue(currentEmployee, currentProject);

		// Add new issue
		if (!(currentEmployee instanceof Developer)) {
			listIssueView.addAddButtonActionListerner(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					new NewIssueController(currentProject, listIssueView);
				}
			});
		}

		// Show issue detail
		listIssueView.addViewButtonActionListerner(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				IssueDetail issueDetail = new IssueDetail(listIssueView.getSelectedIssue(), currentEmployee);

				Issue currentIssue = listIssueView.getSelectedIssue();

				String currentMessage = issueDetail.getMessage();

				// Reload button action
				issueDetail.addReloadButtonActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						issueDetail.setMessage(currentMessage);
					}
				});

				// Save message to db
				issueDetail.addSaveButtonActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						// Protect user from editing others' issue
						if (currentIssue.getAssignee().getId() != currentEmployee.getId()
								&& currentIssue.getReporter().getId() != currentEmployee.getId()
								&& !(currentEmployee instanceof Manager)) {
							JOptionPane.showMessageDialog(null, "You are not allowed to edit issues that's not yours.");
							return;
						}
						currentIssue.setDescription(issueDetail.getMessage());
						currentIssue.setUnread(true);
						currentProject.editIssue(currentIssue);

						// Append to file
						DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						String history = "[" + df.format(new Date()) + "]\n";
						history += "Reporter: "
								+ currentIssue.getReporter().getName() + "\n";
						history += "Assignee: "
								+ currentIssue.getAssignee().getName() + "\n";
						history += "Content: " + currentIssue.getDescription()
								+ "\n\n";
						DatabaseHelper.writeToIssueHistoryFile(currentIssue.getName(), history);

						JOptionPane.showMessageDialog(null, "Message saved to database.");
					}
				});

				// Send button action
				issueDetail.addSendButtonActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						// Protect user from editing others' issue
						if (currentIssue.getAssignee().getId() != currentEmployee.getId()
								&& currentIssue.getReporter().getId() != currentEmployee.getId()
								&& !(currentEmployee instanceof Manager)) {
							JOptionPane.showMessageDialog(null, "You are not allowed to edit issues that's not yours.");
							return;
						}
						currentIssue.setDescription(issueDetail.getMessage());
						currentIssue.setUnread(true);

						if (currentEmployee instanceof Developer) {
							currentIssue.setStatus(ISSUE_STATUS.CHECKING);
						} else {
							currentIssue.setStatus(ISSUE_STATUS.NEW);
						}

						if (currentEmployee.getId() == currentIssue.getAssignee().getId()) {
							Employee temp = currentIssue.getAssignee();
							currentIssue.setAssignee(currentIssue.getReporter());
							currentIssue.setReporter(temp);
						}

						currentProject.editIssue(currentIssue);

						// Append to file
						DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
						String history = "[" + df.format(new Date()) + "]\n";
						history += "Reporter: "
								+ currentIssue.getReporter().getName() + "\n";
						history += "Assignee: "
								+ currentIssue.getAssignee().getName() + "\n";
						history += "Content: " + currentIssue.getDescription()
								+ "\n\n";
						DatabaseHelper.writeToIssueHistoryFile(currentIssue.getName(), history);

						JOptionPane.showMessageDialog(null, "Sent successfully");
						listIssueView.refreshTable(currentProject);
						issueDetail.dispose();
					}
				});

				// Close issue button
				if (!(currentEmployee instanceof Developer)) {
					issueDetail.addCloseButtonActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							currentIssue.setStatus(ISSUE_STATUS.CLOSED);
							currentProject.editIssue(currentIssue);

							// Append to file
							DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
							String history = "[" + df.format(new Date())
									+ "]\n";
							history += "[Issue closed]";
							DatabaseHelper.writeToIssueHistoryFile(currentIssue.getName(), history);

							JOptionPane.showMessageDialog(null, "Issue solved, now being closed.");
							issueDetail.dispose();
						}
					});
				}

				issueDetail.setVisible(true);

			}
		});

		// History button
		listIssueView.addHistoryButtonActionListerner(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				File file = new File(listIssueView.getSelectedIssue().getName()
						+ ".txt");
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				byte[] data = new byte[(int) file.length()];
				try {
					fis.read(data);
					fis.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				try {
					String content = new String(data, "UTF-8");
					new IssueHistory(listIssueView.getSelectedIssue(), content).setVisible(true);
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});

		listIssueView.setVisible(true);
	}
}
