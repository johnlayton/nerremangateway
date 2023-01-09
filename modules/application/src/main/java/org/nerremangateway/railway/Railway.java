package org.nerremangateway.railway;


import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static org.nerremangateway.railway.Railway.Resolvers.successes;
import static org.nerremangateway.railway.Railway.Transforms.*;

public class Railway {

    public static void main(String[] args) {
//        String check = failure("john").either(
//                success -> "Yes",
//                failure -> "No"
//        );
//
//        System.out.println("Check = " + check);
//
//        check = Optional.of("")
//                .map(success())
//                .orElseGet(() -> failure(""))
//                .either(
//                        success -> "Yes",
//                        failure -> "No"
//                );
//
//        System.out.println("Check = " + check);


//        String check = success("input")
//                .then(onAttempt(tryTo(Railway.transformA(true), Throwable::getMessage)))
//                .then(onAttempt(tryTo(Railway::transformB, Throwable::getMessage)))
//                .either(
//                        success -> success.toString(),
//                        failure -> failure.toString()
//                );
//
//        System.out.println("Check = " + check);

//    public static class TransformException extends Exception {
//        public TransformException(String message) {
//            super(message);
//        }
//    }
//
//    public static ThrowingFunction<String, String, TransformException> transformA(final Boolean fail) {
//        return fail
//                ? input -> { throw new TransformException("Message"); }
//                : input -> { return "transform-a-" + input; };
//    }
//
//    public static String transformB(final String input) throws TransformException {
//        return "transform-b-" + input;
//    }

        final List<StudentImport> students = List.of(
                new StudentImport("a@a.com", "1", "john", "doe"),
                new StudentImport("a@a.com", "1", "john", "doe"),
                new StudentImport("b@a.com", "2", "jane", "doe"),
                new StudentImport("c@a.com", "3", "betty", "doe")
        );

        final List<StudentMapping> mappings = List.of(
                new StudentMapping("email", "email"),
                new StudentMapping("code", "code"),
                new StudentMapping("firstName", "firstName"),
                new StudentMapping("lastName", "lastName")
        );

        new StudentImportService(new StudentRecordRepository())
                .importStudents(students, mappings)
                .forEach(result -> {
                    System.out.println("Result = " + result);
                });

    }


    public record StudentMapping(String left, String right) {
    }

    public record StudentImport(String email, String code, String firstName, String lastName) {
    }

    public record StudentRecord(String email, String code, String firstName, String lastName) {
    }

    public record ImportError(String message) {
    }

    public abstract static sealed class StudentImportResult<T> permits StudentImportResultSuccess, StudentImportResultFailed {
        private final StudentImport studentImport;
        private final T result;

        protected StudentImportResult(StudentImport studentImport, T result) {
            this.studentImport = studentImport;
            this.result = result;
        }

        public StudentImport getStudentImport() {
            return studentImport;
        }

        public T getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "StudentImportResult{studentImport=" + studentImport + ", result=" + result + '}';
        }
    }

    public static final class StudentImportResultSuccess extends StudentImportResult<StudentRecord> {
        public StudentImportResultSuccess(StudentImport studentImport, StudentRecord result) {
            super(studentImport, result);
        }
    }

    public static final class StudentImportResultFailed extends StudentImportResult<ImportError> {
        public StudentImportResultFailed(StudentImport studentImport, ImportError error) {
            super(studentImport, error);
        }
    }

    public static class StudentImportService {
        private final StudentRecordRepository repository;

        public StudentImportService(final StudentRecordRepository repository) {
            this.repository = repository;
        }

        public List<StudentImportResult> importStudents(final List<StudentImport> students,
                                                        final List<StudentMapping> mappings) {
            final StudentImportValidator validator = new StudentImportValidator(students);
            return students.stream()
//                    .map(tryTo(validateDistinct(students), handler()))
                    .map(Throwing.tryTo(validator::validateDistinct))
                    .map(onSuccessTry(this::validateEmail))
                    .map(onSuccessTry(StudentImportService::createSuccess))
                    .map(recover(Recovery.ifType(DuplicateImportException.class, duplicate -> {
                        return new StudentImportResultFailed(duplicate.getSource(), new ImportError(duplicate.getMessage()));
                    })))
                    .map(recover(Recovery.ifType(InvalidEmailException.class, duplicate -> {
                        return new StudentImportResultFailed(duplicate.getSource(), new ImportError(duplicate.getMessage()));
                    })))
                    .flatMap(successes())
                    .collect(Collectors.toList());


            //                    .map(onSuccessTry())
//                    .map(onSuccessTry(this::validateEmail))
//                    .map(onSuccessTry(validateEmail()))
//                    .map(tryTo(validateEmail()), handler())
//                    .map(tryTo(validate(students)))
//                    .map(ifFailed())
//                    .map(onFailure(failure -> {
//                        System.out.println("Failed: " + failure);
////                        return new StudentImportResultFailed(studentImport, new StudentRecord(
////                                studentImport.email(),
////                                studentImport.code(),
////                                studentImport.firstName(),
////                                studentImport.lastName()
////                        ));
//                    }))
//                    .flatMap(all())
//                    .flatMap(successes())
//                    .flatMap(failures())
//                    .map()
//                    .collect(Collectors.toList());

//            return result;
        }

        private static StudentImportResult createSuccess(StudentImport studentImport) {
            return new StudentImportResultSuccess(studentImport, new StudentRecord(
                    studentImport.email(),
                    studentImport.code(),
                    studentImport.firstName(),
                    studentImport.lastName()
            ));
        }

//        private Function<StudentImport, Result<StudentImport, String>> validate(final List<StudentImport> students) {
//            return studentImport -> {
//                return studentImport.code().equals("1")
//                        ? success(studentImport)
//                        : failure("Bob");
//            };
//        }

        private Throwing.ThrowingFunction<StudentImport, StudentImport, DuplicateImportException> validateDistinct(final List<StudentImport> students) {
            return current -> {
                final Optional<StudentImport> other = students.stream()
                        .filter(o -> o != current)
                        .filter(o -> o.code().equals(current.code()))
                        .findFirst();
                if (other.isPresent())
                    throw new DuplicateImportException(current, other.get());
                else
                    return current;
//                return studentImport.code().equals("1") ? studentImport : studentImport;
            };
        }

//        private ThrowingFunction<StudentImport, StudentImport, InvalidEmailException> validateEmail() {
//            return current -> {
//                if (current.email().equals("c@a.com"))
//                    throw new InvalidEmailException(current);
//                else
//                    return current;
//            };
//        }

        private class StudentImportValidator {
            private final List<StudentImport> students;

            public StudentImportValidator(List<StudentImport> students) {
                this.students = students;
            }

            private StudentImport validateDistinct(StudentImport studentImport) throws DuplicateImportException {
//                if (studentImport.email().equals("c@a.com"))
//                    throw new DuplicateImportException(studentImport);
//                else
//                    return studentImport;
                final Optional<StudentImport> other = students.stream()
                        .filter(o -> o != studentImport)
                        .filter(o -> o.code().equals(studentImport.code()))
                        .findFirst();
                if (other.isPresent())
                    throw new DuplicateImportException(studentImport, other.get());
                else
                    return studentImport;
            }
        }

        private StudentImport validateEmail(StudentImport studentImport) throws InvalidEmailException {
            if (studentImport.email().equals("c@a.com"))
                throw new InvalidEmailException(studentImport);
            else
                return studentImport;
        }

        private BiFunction<StudentImport, Exception, StudentImportResultFailed> handler() {
            return (input, exception) -> new StudentImportResultFailed(input, new ImportError(exception.getMessage()));
        }


//        private ThrowingConsumer<StudentImport, DuplicateRecordException> validateCode(final List<StudentImport> students) {
//            return student -> students.stream()
//                    .filter(existing -> student.code().equals(existing.code))
//                    .findFirst()
//                    .isPresent()
//                    ? throw new
//        }

        public static class InvalidEmailException extends Exception {

            private final StudentImport source;

            public InvalidEmailException(StudentImport source) {
                super(String.format("Invalid StudentImport Email Address %s", source.email()));
                this.source = source;
            }


            public StudentImport getSource() {
                return source;
            }
        }

        public static class DuplicateImportException extends Exception {
            private final StudentImport source;
            private final StudentImport target;

            public DuplicateImportException(StudentImport source, StudentImport target) {
                super(String.format("Duplicate StudentImport Records Supplied %s %s", source, target));
                this.source = source;
                this.target = target;
            }

            public StudentImport getSource() {
                return source;
            }

            public StudentImport getTarget() {
                return target;
            }
        }
    }

    public static class StudentRecordRepository {
        public List<StudentRecord> findAllStudents() {
            return List.of(
                    new StudentRecord("a@a.com", "1", "john", "doe"),
                    new StudentRecord("b@a.com", "2", "jane", "doe")
            );
        }

        public StudentRecord save(final StudentImport student) {
            return new StudentRecord(student.email(), student.code(), student.firstName(), student.lastName());
        }
    }

    public sealed interface Result<S, F> permits Result.Success, Result.Failure {

        <R, R1 extends R, R2 extends R> R either(Function<S, R1> onSuccess, Function<F, R2> onFailure);

        default <T, T2 extends T> T then(Function<Result<S, F>, T2> mapper) {
            return mapper.apply(this);
        }

        static <S, F> Function<S, Result<S, F>> success() {
            return Result::success;
        }

        static <S, F> Result<S, F> success(S value) {
            return new Success<>(value);
        }

        static <S, F> Function<F, Result<S, F>> failure() {
            return Result::failure;
        }

        static <S, F> Result<S, F> failure(F value) {
            return new Failure<>(value);
        }

        record Failure<L, R>(R failure) implements Result<L, R> {
            @Override
            public <S, S1 extends S, S2 extends S> S either(Function<L, S1> ignored, Function<R, S2> onFailure) {
                return onFailure.apply(failure);
            }
        }

        record Success<L, R>(L success) implements Result<L, R> {
            @Override
            public <S, S1 extends S, S2 extends S> S either(Function<L, S1> onSuccess, Function<R, S2> ignored) {
                return onSuccess.apply(success);
            }
        }

//        interface ConsumableFunction<A> extends Function<A, A>, Consumer<A> {
//
//            default A apply(final A value) {
//                accept(value);
//                return value;
//            }
//        }
    }

    public interface Resolvers {
        static <S, F> Function<Result<S, F>, Stream<S>> successes() {
            return result -> result.either(
                    success -> Stream.of(success),
                    failure -> Stream.empty()
            );
        }


        static <S, F> Function<Result<S, F>, Stream<F>> failures() {
            return result -> result.either(
                    success -> Stream.empty(),
                    failure -> Stream.of(failure)
            );
        }


        static <S extends T, F extends T, T> Function<Result<S, F>, Stream<T>> all() {
            return result -> result.either(
                    success -> Stream.of(success),
                    failure -> Stream.of(failure)
            );
        }

        static <S, F> Function<Result<S, F>, S> ifFailed(Function<F, S> function) {
/*
        return result -> result.either(identity(), function);
*/
            return result -> result.either(
                    success -> success,
                    failure -> function.apply(failure)
            );
        }
    }

    public interface Introduction {
        static <IS, OS extends IS> Function<IS, Result<OS, IS>> castTo(Class<OS> targetClass) {
            return input -> targetClass.isAssignableFrom(input.getClass())
                    ? Result.success(targetClass.cast(input))
                    : Result.failure(input);
        }
    }

    public interface Recovery {
        static <S, F, TF extends F> Function<F, Result<S, F>> ifType(Class<TF> targetClass, Function<TF, S> mapper) {
            return Introduction.<F, TF>castTo(targetClass).andThen(onSuccess(mapper));
        }
    }

    public interface Transforms {
        static <S, F> Function<Result<S, F>, Result<S, F>> onFailure(Consumer<F> consumer) {
            return onFailure(Functions.peek(consumer));
/*
        return result -> result.either(
            success(),
            failure -> failure(peek(consumer).apply(failure))
        );
*/
/*
        return result -> result.either(
            success -> success(success),
            failure -> failure(peek(consumer).apply(failure))
        );
*/
/*
        return result -> result.either(
                success -> Result.success(success),
                failure -> {
                    consumer.accept(failure);
                    return failure(failure);
                }
        );
*/
        }

        static <S, T, F> Function<Result<S, F>, Result<T, F>> onSuccess(Function<S, T> function) {
            return result -> result.either(
                    success -> Result.success(function.apply(success)),
                    failure -> Result.failure(failure)
            );
        }

        static <IS, OS, X extends Exception> Function<Result<IS, Exception>, Result<OS, Exception>> onSuccessTry(
                Throwing.ThrowingFunction<IS, OS, X> throwingFunction
        ) {
            return attempt(Throwing.tryTo(throwingFunction));
        }

        static <S, F, T> Function<Result<S, F>, Result<S, T>> onFailure(Function<F, T> function) {
            return result -> result.either(
                    success -> Result.success(success),
                    failure -> Result.failure(function.apply(failure))
            );
        }

        static <IS, OS, F, OF extends F> Function<Result<IS, F>, Result<OS, F>> attempt(Function<IS, Result<OS, OF>> mappingFunction) {
            return r -> r.either(
                    mappingFunction.andThen(onFailure((Function<OF, F>) Transforms::upcast)),
                    Result::failure
            );
//            return r -> r.either(mappingFunction.andThen(onFailure((Function<OF, F>) (fv) -> upcast(fv))), Result::failure);
        }

        static <R, T extends R> R upcast(T fv) {
            return fv;
        }

        static <S, T, F> Function<Result<S, F>, Result<T, F>> onAttempt(Function<S, Result<T, F>> function) {
            return result -> result.either(
                    success -> function.apply(success),
                    failure -> Result.failure(failure)
            );
        }

        static <S, IF, OF, OS extends S> Function<Result<S, IF>, Result<S, OF>> recover(Function<IF, Result<OS, OF>> recoveryFunction) {
            return r -> r.either(Result::success, recoveryFunction.andThen(onSuccess((Function<OS, S>) (fv) -> Transforms.upcast(fv))));
        }

    }

    public interface Functions {
        static <T> Function<T, T> peek(Consumer<T> consumer) {
            return value -> {
                consumer.accept(value);
                return value;
            };
        }
    }

    public interface Throwing {

        static <S, T, X extends Exception> Function<S, Result<T, Exception>> tryTo(ThrowingFunction<S, T, X> throwingFunction) {
            return tryTo(throwingFunction, identity());
        }

        static <S, T, X extends Exception, F> Function<S, Result<T, F>> tryTo(ThrowingFunction<S, T, X> throwingFunction, F failureCase) {
            return tryTo(throwingFunction, ignored -> failureCase);
        }

        static <S, T, X extends Exception, F> Function<S, Result<T, F>> tryTo(ThrowingFunction<S, T, X> throwingFunction, Function<Exception, F> exceptionMapper) {
            return input -> {
                try {
                    return Result.success(throwingFunction.apply(input));
                } catch (Exception ex) {
                    return Result.failure(exceptionMapper.apply(ex));
                }
            };
        }

        static <S, T, X extends Exception, F> Function<S, Result<T, F>> tryTo(ThrowingFunction<S, T, X> throwingFunction, BiFunction<S, Exception, F> exceptionMapper) {
            return input -> {
                try {
                    return Result.success(throwingFunction.apply(input));
                } catch (final Exception exception) {
                    return Result.failure(exceptionMapper.apply(input, exception));
                }
            };

        }

//    static <S, T, X extends Exception, F> Function<S, Result<T, F>> tryTo(ThrowingConsumer<T, X> throwingConsumer, Function<Exception, F> exceptionMapper) {
//        return r -> r.then(onSuccess(Result.peek(throwingConsumer)));
////        return input -> {
////            try {
////                throwingConsumer.accept(input);
////                return success(throwingFunction.apply(input));
////            } catch (Exception ex) {
////                return failure(exceptionMapper.apply(ex));
////            }
////        };
//    }

        @FunctionalInterface
        interface ThrowingFunction<I, O, X extends Exception> {
            O apply(I input) throws X;

            default <T> ThrowingFunction<I, T, X> andThen(Function<O, T> nextFunction) {
                return x -> nextFunction.apply(apply(x));
            }

            default <T> ThrowingFunction<T, O, X> compose(Function<T, I> nextFunction) {
                return x -> apply(nextFunction.apply(x));
            }

            static <I, O, X extends Exception> Function<I, O> throwingRuntime(ThrowingFunction<I, O, X> function) {
                return input -> {
                    try {
                        return function.apply(input);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                };
            }
        }

        @FunctionalInterface
        interface ThrowingConsumer<T, X extends Exception> {
            void accept(T item) throws X;

            static <T, X extends Exception> Consumer<T> throwingRuntime(ThrowingConsumer<T, X> p) {
                return x -> {
                    try {
                        p.accept(x);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                };
            }
        }
    }
}
