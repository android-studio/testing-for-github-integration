package com.jg.videonotebook.dao;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig notebookDaoConfig;
    private final DaoConfig videoNoteDaoConfig;

    private final NotebookDao notebookDao;
    private final VideoNoteDao videoNoteDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        notebookDaoConfig = daoConfigMap.get(NotebookDao.class).clone();
        notebookDaoConfig.initIdentityScope(type);

        videoNoteDaoConfig = daoConfigMap.get(VideoNoteDao.class).clone();
        videoNoteDaoConfig.initIdentityScope(type);

        notebookDao = new NotebookDao(notebookDaoConfig, this);
        videoNoteDao = new VideoNoteDao(videoNoteDaoConfig, this);

        registerDao(Notebook.class, notebookDao);
        registerDao(VideoNote.class, videoNoteDao);
    }
    
    public void clear() {
        notebookDaoConfig.getIdentityScope().clear();
        videoNoteDaoConfig.getIdentityScope().clear();
    }

    public NotebookDao getNotebookDao() {
        return notebookDao;
    }

    public VideoNoteDao getVideoNoteDao() {
        return videoNoteDao;
    }

}
