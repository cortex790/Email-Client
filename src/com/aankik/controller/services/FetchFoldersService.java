package com.aankik.controller.services;

import com.aankik.model.EmailTreeItem;
import com.aankik.view.IconResolver;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import java.util.List;

public class FetchFoldersService extends Service<Void> {

    private Store store;
    private EmailTreeItem<String> foldersRoot;
    private List<Folder> folderList;
    private IconResolver iconResolver = new IconResolver();


    public FetchFoldersService(Store store, EmailTreeItem<String> foldersRoot, List<Folder> folderList) {
        this.store = store;
        this.foldersRoot = foldersRoot;
        this.folderList = folderList;
    }


    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                fetchFolders();
                return null;
            }
        };
    }

    private void fetchFolders() throws MessagingException {
        Folder[] folders = store.getDefaultFolder().list();
        handleFolders(folders, foldersRoot);
    }

    private void handleFolders(Folder[] folders, EmailTreeItem<String> foldersRoot) throws MessagingException {
        for (Folder folder : folders) {
            folderList.add(folder);//to update the new email folder
            EmailTreeItem<String> emailTreeItem = new EmailTreeItem<String>(folder.getName());
            emailTreeItem.setGraphic(iconResolver.getIconForFolder(folder.getName()));//setting icon in tree view
            foldersRoot.getChildren().add((emailTreeItem));
            foldersRoot.setExpanded(true);
            addMessageListenerToFolder(folder, emailTreeItem);

                fetchMessagesOnFolder(folder, emailTreeItem);
                if (folder.getType() == Folder.HOLDS_FOLDERS) {
                    Folder[] subFolders = folder.list();
                    handleFolders(subFolders, emailTreeItem);
                }
            }

        }

        //adding new folders
    private void addMessageListenerToFolder(Folder folder, EmailTreeItem<String> emailTreeItem) {
        folder.addMessageCountListener(new MessageCountListener() {
            @Override
            public void messagesAdded(MessageCountEvent messageCountEvent) {
                for (int i = 0; i < messageCountEvent.getMessages().length; i++){
                    try {
                        Message message = folder.getMessage(folder.getMessageCount() - i);
                        emailTreeItem.addEmailToTop(message);
                    } catch (MessagingException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void messagesRemoved(MessageCountEvent messageCountEvent) {

            }
        });
    }

    private void fetchMessagesOnFolder (Folder folder, EmailTreeItem < String > emailTreeItem){
            Service fetchMessagesService = new Service() {
                @Override
                protected Task createTask() {
                    return new Task() {
                        @Override
                        protected Object call() throws Exception {
                            if (folder.getType() != Folder.HOLDS_FOLDERS) {
                                folder.open(Folder.READ_WRITE);
                                int folderSize = folder.getMessageCount();
                                for (int i = folderSize; i > 0; i--) {
                                    emailTreeItem.addEmail(folder.getMessage(i));
                                }
                            }
                            return null;
                        }
                    };
                }
            };
            fetchMessagesService.start();
        }
    }

