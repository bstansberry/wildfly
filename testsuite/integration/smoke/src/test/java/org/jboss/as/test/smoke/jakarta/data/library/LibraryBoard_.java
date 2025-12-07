package org.jboss.as.test.smoke.jakarta.data.library;

import java.util.List;
import java.util.Optional;

import jakarta.annotation.Generated;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.StatelessSession;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.as.test.smoke.jakarta.data.lib.Book;
import org.jboss.as.test.smoke.jakarta.data.lib.Book_;
import org.jboss.as.test.smoke.jakarta.data.lib.Librarian;
import org.jboss.as.test.smoke.jakarta.data.lib.Library;
import org.jboss.as.test.smoke.jakarta.data.lib.Library_;

@Dependent
@Generated("org.hibernate.processor.HibernateProcessor")
public class LibraryBoard_ implements LibraryBoard {

    static final String FIND_BOOKS_BY_AUTHOR_NAME_String = "from Book where author.person.name like :name";


    @Override
    public Library openLibrary(@Nonnull Library library) {
        if (library == null) throw new IllegalArgumentException("Null library");
        try {
            session.insert(library);
            return library;
        } catch (ConstraintViolationException exception) {
            throw new EntityExistsException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    protected @Nonnull StatelessSession session;

    public LibraryBoard_(@Nonnull StatelessSession session) {
        this.session = session;
    }

    public @Nonnull StatelessSession session() {
        return session;
    }

    /**
     * Execute the query {@value #FIND_BOOKS_BY_AUTHOR_NAME_String}.
     *
     * @see LibraryBoard#findBooksByAuthorName(String)
     **/
    @Override
    public List<Book> findBooksByAuthorName(String name) {
        try {
            return session.createSelectionQuery(FIND_BOOKS_BY_AUTHOR_NAME_String, Book.class)
                    .setParameter("name", name)
                    .getResultList();
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public void updateLibrary(@Nonnull Library library) {
        if (library == null) throw new IllegalArgumentException("Null library");
        try {
            session.update(library);
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public void closeLibrary(@Nonnull Library library) {
        if (library == null) throw new IllegalArgumentException("Null library");
        try {
            session.delete(library);
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    /**
     * Find {@link Book} by {@link Book#title title}.
     *
     * @see LibraryBoard#findBook(String)
     **/
    @Override
    public Optional<Book> findBook(@Nonnull String title) {
        if (title == null) throw new IllegalArgumentException("Null title");
        var _builder = session.getFactory().getCriteriaBuilder();
        var _query = _builder.createQuery(Book.class);
        var _entity = _query.from(Book.class);
        _query.where(
                _builder.equal(_entity.get(Book_.title), title)
        );
        try {
            return session.createSelectionQuery(_query)
                    .uniqueResultOptional();
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public void fireLibrarian(@Nonnull Librarian librarian) {
        if (librarian == null) throw new IllegalArgumentException("Null librarian");
        try {
            session.delete(librarian);
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    /**
     * Find {@link Library} by {@link Library#name name}.
     *
     * @see LibraryBoard#findLibrary(String)
     **/
    @Override
    public Optional<Library> findLibrary(@Nonnull String name) {
        if (name == null) throw new IllegalArgumentException("Null name");
        var _builder = session.getFactory().getCriteriaBuilder();
        var _query = _builder.createQuery(Library.class);
        var _entity = _query.from(Library.class);
        _query.where(
                _builder.equal(_entity.get(Library_.name), name)
        );
        try {
            return session.createSelectionQuery(_query)
                    .uniqueResultOptional();
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public Librarian hireLibrarian(@Nonnull Librarian librarian) {
        if (librarian == null) throw new IllegalArgumentException("Null librarian");
        try {
            session.insert(librarian);
            return librarian;
        } catch (ConstraintViolationException exception) {
            throw new EntityExistsException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @PersistenceUnit
    private EntityManagerFactory sessionFactory;

    @PostConstruct
    private void openSession() {
        session = sessionFactory.unwrap(SessionFactory.class).openStatelessSession();
    }

    @PreDestroy
    private void closeSession() {
        session.close();
    }

    @Inject
    LibraryBoard_() {
    }

}

