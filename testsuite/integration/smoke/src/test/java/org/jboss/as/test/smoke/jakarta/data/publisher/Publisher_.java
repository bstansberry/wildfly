package org.jboss.as.test.smoke.jakarta.data.publisher;

import static org.hibernate.query.Order.by;
import static org.hibernate.query.SortDirection.ASCENDING;

import java.util.ArrayList;
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
import org.hibernate.query.Order;
import org.jboss.as.test.smoke.jakarta.data.lib.Author;
import org.jboss.as.test.smoke.jakarta.data.lib.Book;
import org.jboss.as.test.smoke.jakarta.data.lib.Book_;

@Dependent
@Generated("org.hibernate.processor.HibernateProcessor")
public class Publisher_ implements Publisher {

    static final String FIND_AUTHOR_BY_NAME_String = "select author from Author author inner join author.person person where person.name = :name";


    @Override
    public Author signAuthor(@Nonnull Author author) {
        if (author == null) throw new IllegalArgumentException("Null author");
        try {
            session.insert(author);
            return author;
        } catch (ConstraintViolationException exception) {
            throw new EntityExistsException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    protected @Nonnull StatelessSession session;

    public Publisher_(@Nonnull StatelessSession session) {
        this.session = session;
    }

    public @Nonnull StatelessSession session() {
        return session;
    }

    @Override
    public Author updateAuthor(@Nonnull Author author) {
        if (author == null) throw new IllegalArgumentException("Null author");
        try {
            session.update(author);
            return author;
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    /**
     * Find {@link Book} by {@link Book#title title}.
     *
     * @see Publisher#findBookByTitle(String)
     **/
    @Override
    public Optional<Book> findBookByTitle(@Nonnull String title) {
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

    /**
     * Execute the query {@value #FIND_AUTHOR_BY_NAME_String}.
     *
     * @see Publisher#findAuthorByName(String)
     **/
    @Override
    public Optional<Author> findAuthorByName(String name) {
        try {
            return session.createSelectionQuery(FIND_AUTHOR_BY_NAME_String, Author.class)
                    .setParameter("name", name)
                    .uniqueResultOptional();
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    /**
     * Find {@link Book}.
     *
     * @see Publisher#booksByPageCount()
     **/
    @Override
    public List<Book> booksByPageCount() {
        var _builder = session.getFactory().getCriteriaBuilder();
        var _query = _builder.createQuery(Book.class);
        var _entity = _query.from(Book.class);
        _query.where(
        );
        var _orders = new ArrayList<Order<? super Book>>();
        _orders.add(by(Book.class, "pageCount", ASCENDING, false));
        try {
            return session.createSelectionQuery(_query)
                    .setOrder(_orders)
                    .getResultList();
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
    Publisher_() {
    }

    @Override
    public Book publish(@Nonnull Book newBook) {
        if (newBook == null) throw new IllegalArgumentException("Null newBook");
        try {
            session.insert(newBook);
            return newBook;
        } catch (ConstraintViolationException exception) {
            throw new EntityExistsException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

}

