package org.jboss.as.test.smoke.jakarta.data.lib;

import static java.util.Optional.ofNullable;
import static org.hibernate.query.Order.by;
import static org.hibernate.query.SortDirection.ASCENDING;
import static org.hibernate.query.SortDirection.DESCENDING;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.annotation.Generated;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.data.Order;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.impl.PageRecord;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnit;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.StatelessSession;
import org.hibernate.exception.ConstraintViolationException;

@Dependent
@Generated("org.hibernate.processor.HibernateProcessor")
public class Recruiter_ implements Recruiter {


    @Override
    public void delete(@Nonnull Person entity) {
        if (entity == null) throw new IllegalArgumentException("Null entity");
        try {
            session.delete(entity);
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    /**
     * Find {@link Person} by {@link Person#id id}.
     *
     * @see Recruiter#deleteById(Long)
     **/
    @Override
    public void deleteById(@Nonnull Long id) {
        if (id == null) throw new IllegalArgumentException("Null id");
        var _builder = session.getFactory().getCriteriaBuilder();
        var _query = _builder.createCriteriaDelete(Person.class);
        var _entity = _query.from(Person.class);
        _query.where(
                _builder.equal(_entity.get(Person_.id), id)
        );
        try {
            session.createMutationQuery(_query)
                    .executeUpdate();
        } catch (NoResultException exception) {
            throw new EmptyResultException(exception.getMessage(), exception);
        } catch (NonUniqueResultException exception) {
            throw new jakarta.data.exceptions.NonUniqueResultException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    /**
     * Find {@link Person} by {@link Person#name name} and {@link Person#birthdate birthdate}.
     *
     * @see Recruiter#find(String, LocalDate)
     **/
    @Override
    public Optional<Person> find(@Nonnull String name, @Nonnull LocalDate birthdate) {
        if (name == null) throw new IllegalArgumentException("Null name");
        if (birthdate == null) throw new IllegalArgumentException("Null birthdate");
        var _builder = session.getFactory().getCriteriaBuilder();
        var _query = _builder.createQuery(Person.class);
        var _entity = _query.from(Person.class);
        _query.where(
                _builder.equal(_entity.get(Person_.name), name),
                _builder.equal(_entity.get(Person_.birthdate), birthdate)
        );
        try {
            return session.createSelectionQuery(_query)
                    .uniqueResultOptional();
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    protected @Nonnull StatelessSession session;

    public Recruiter_(@Nonnull StatelessSession session) {
        this.session = session;
    }

    public @Nonnull StatelessSession session() {
        return session;
    }

    /**
     * Find {@link Person} by {@link Person#name name}.
     *
     * @see Recruiter#find(String)
     **/
    @Override
    public Optional<Person> find(@Nonnull String name) {
        if (name == null) throw new IllegalArgumentException("Null name");
        var _builder = session.getFactory().getCriteriaBuilder();
        var _query = _builder.createQuery(Person.class);
        var _entity = _query.from(Person.class);
        _query.where(
                _builder.equal(_entity.get(Person_.name), name)
        );
        try {
            return session.createSelectionQuery(_query)
                    .uniqueResultOptional();
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public Person update(@Nonnull Person entity) {
        if (entity == null) throw new IllegalArgumentException("Null entity");
        try {
            session.update(entity);
            return entity;
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    /**
     * Find {@link Person}.
     *
     * @see Recruiter#findAll(PageRequest, Order)
     **/
    @Override
    public Page<Person> findAll(PageRequest pageRequest, Order<Person> sortBy) {
        var _builder = session.getFactory().getCriteriaBuilder();
        var _query = _builder.createQuery(Person.class);
        var _entity = _query.from(Person.class);
        _query.where(
        );
        var _orders = new ArrayList<org.hibernate.query.Order<? super Person>>();
        for (var _sort : sortBy.sorts()) {
            _orders.add(by(Person.class, _sort.property(),
                    _sort.isAscending() ? ASCENDING : DESCENDING,
                    _sort.ignoreCase()));
        }
        try {
            long _totalResults =
                    pageRequest.requestTotal()
                            ? session.createSelectionQuery(_query)
                            .getResultCount()
                            : -1;
            var _results = session.createSelectionQuery(_query)
                    .setFirstResult((int) (pageRequest.page() - 1) * pageRequest.size())
                    .setMaxResults(pageRequest.size())
                    .setOrder(_orders)
                    .getResultList();
            return new PageRecord(pageRequest, _results, _totalResults);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    /**
     * Find {@link Person}.
     *
     * @see Recruiter#findAll()
     **/
    @Override
    public Stream<Person> findAll() {
        var _builder = session.getFactory().getCriteriaBuilder();
        var _query = _builder.createQuery(Person.class);
        var _entity = _query.from(Person.class);
        _query.where(
        );
        try {
            return session.createSelectionQuery(_query)
                    .getResultStream();
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public List updateAll(@Nonnull List entities) {
        if (entities == null) throw new IllegalArgumentException("Null entities");
        try {
            for (var _entity : entities) {
                session.update(_entity);
            }
            return entities;
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public Person insert(@Nonnull Person entity) {
        if (entity == null) throw new IllegalArgumentException("Null entity");
        try {
            session.insert(entity);
            return entity;
        } catch (ConstraintViolationException exception) {
            throw new EntityExistsException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public List insertAll(@Nonnull List entities) {
        if (entities == null) throw new IllegalArgumentException("Null entities");
        try {
            for (var _entity : entities) {
                session.insert(_entity);
            }
            return entities;
        } catch (ConstraintViolationException exception) {
            throw new EntityExistsException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public List saveAll(@Nonnull List entities) {
        if (entities == null) throw new IllegalArgumentException("Null entities");
        try {
            for (var _entity : entities) {
                session.upsert(_entity);
            }
            return entities;
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public Person save(@Nonnull Person entity) {
        if (entity == null) throw new IllegalArgumentException("Null entity");
        try {
            if (session.getIdentifier(entity) == null)
                session.insert(entity);
            else
                session.upsert(entity);
            return entity;
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    @Override
    public void deleteAll(@Nonnull List<? extends Person> entities) {
        if (entities == null) throw new IllegalArgumentException("Null entities");
        try {
            for (var _entity : entities) {
                session.delete(_entity);
            }
        } catch (StaleStateException exception) {
            throw new OptimisticLockingFailureException(exception.getMessage(), exception);
        } catch (PersistenceException exception) {
            throw new DataException(exception.getMessage(), exception);
        }
    }

    /**
     * Find {@link Person} by {@link Person#id id}.
     *
     * @see Recruiter#findById(Long)
     **/
    @Override
    public Optional<Person> findById(@Nonnull Long id) {
        if (id == null) throw new IllegalArgumentException("Null id");
        try {
            return ofNullable(session.get(Person.class, id));
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
    Recruiter_() {
    }

}

