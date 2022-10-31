# Student Service Application

The purpose of this project is to demonstrate testing of a simple spring rest application.  

This is a rest api that saves and returns student data to, and from a mysql db.

This project covers unit tests through to end-to-end tests, and hopefully it helps to get you familiar
with testing spring framework applications.

## Running the application

If you want to run the app using docker, first, build the application jar using `mvn package`, then 
run the [docker-compose](docker-compose.yml) file with `docker compose up` can be used to create the services locally to run and play around with, including the mysql database.  

This will be available at http://localhost:8081/students.  

You can access the swagger ui at http://localhost:8081/swagger-ui/index.html where you can also play around with the application.

## Workflows on GitHub Actions

For pipeline purposes, I wanted to use as few tools as possible and could not be bothered to host my own CI/CD tool,
and for this reason, GitHub Actions was my choice. 

I won't dive into details on GitHub Actions, as I want to focus on Spring tests here, but a here's a _super_ quick overview of what each workflow file does:

The [branch workflow](.github/workflows/branch-workflow.yml) runs tests on pull requests to the main branch.

The [master workflow](.github/workflows/master-workflow.yml) runs tests on the master branch and, if successful, pushes the docker image to docker repository, which is available to pull from this command `docker pull khanivorous/student-service`


## Application architecture overview

The purpose of this application, is to write the name and age of a student to a database. 
This can also retrieve a list of all the students stored in the database.
It can also retrieve and individual student by their id number or delete a student by their id number. 
If a student id does not exist, then when retrieving or deleting a student by that id number, and exception message is thrown back to the user.
This uses spring jpa library for data persistence.


### Student class
The data to be persisted and retrieved is data about a student. This object contains: name, age and an id number;
The [Student.java](src/main/java/com/khanivorous/studentservice/student/entities/Student.java) class is annotated with `@Entity` indicating that it is a Spring JPA entity. The field id annotated with`@Id` and `@Generated`. This tells jpa that this is an id field and to automatically generate the id.

```java
@Entity
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    // rest of class
}
```

### Student creation DTO class
This class is to represent data processed for the post request from clients [StudentCreationDTO](src/main/java/com/khanivorous/studentservice/student/model/StudentCreationDTO.java)

```java
public record StudentCreationDTO(
        @NotEmpty(message = "name must not be empty")
        String name,
        @NotNull(message = "age must not be null")
        @Min(value = 17, message = "age cannot be less than 17 years old")
        Integer age) {
}
```

### Student DTO class
This is a DTO class for Student object [StudentDTO](src/main/java/com/khanivorous/studentservice/student/model/StudentDTO.java)  

```java
public record StudentDTO(Integer id, String name, Integer age) {
}
```

### Student mapper class
This class maps Student entity class to the StudentDTO class, allowing us to separate entity for persistence and the object for data transfer.
[StudentMapper](src/main/java/com/khanivorous/studentservice/student/mapper/StudentMapper.java)


### Student repository class
This [StudentRepository.java](src/main/java/com/khanivorous/studentservice/student/repository/StudentRepository.java) class is an interface extending Springs CrudRepository. JPA will create an implementation of this interface when running the application.
You can read more on Spring JPA [here](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#reference)

### Student service class
The [StudentServiceImpl.java](src/main/java/com/khanivorous/studentservice/student/services/StudentServiceImpl.java) class uses the student repository and performs some extra business logic.

For example, in this service class, adding a student uses the repository void method `save()`. Here we use save and return a Student object to present back to the user. 
It uses the student mapper class to convert the entity class to a data transfer object.
```java
@Service
public class StudentServiceImpl implements StudentService {

    private StudentRepository studentRepository;

    private StudentMapper studentMapper;

    public StudentServiceImpl(StudentRepository studentRepository, StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
    }

    public StudentDTO addNewStudent(String name, int age) {
        Student newStudent = new Student();
        newStudent.setName(name);
        newStudent.setAge(age);
        return  studentMapper.toDTO(studentRepository.save(newStudent));
    }
    //...rest of class omitted
}
```

The following method returns a Student object when searching by id. Additionally, it throws a custom exception `NoSuchIdException` if that id does not exist.

```java

@Service
public class StudentServiceImpl implements StudentService {
    //...
    public StudentDTO getStudentById(int id) {
        Student student = studentRepository.findById(id).orElseThrow(() -> new NoSuchIdException(id));
        return studentMapper.toDTO(student);
    }
    //...
}
```

We also have a method to delete a Student in the database, by providing the Students id.
It checks if the Student with the given id exists, if not, it throws the custom NoSuchIdException and should not call the repository delete() method.
This allows us to demonstrate a useful feature of Mockito for testing purposes.

```java
@Service
public class StudentServiceImpl implements StudentService {
    //...
    public void deleteStudentById(int id) {
        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
        } else {
            throw new NoSuchIdException(id);
        }
    }
    //...
}
```

### Student controller class
The [StudentController.java](src/main/java/com/khanivorous/studentservice/student/controllers/StudentController.java) class defines the HTTP mappings and the appropriate responses including the exceptions and response codes.
It uses the StudentService class to perform the business logic and returns the response to the user.
For example, when the user adds a user with a post request of an object like 
```json 
{ "name" : "anyName",
  "age: 22
}
``` 
this will respond with a 201 created status code and a json response of the student added:

```java
@RestController
@RequestMapping(path = "/students")
public class StudentController {

    private StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @Operation(summary = "Add a new Student")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Added new student",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = Student.class)) }
            )})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public @ResponseBody
    Student addNewStudent(@Valid @RequestBody StudentCreationDTO student) {
        String name = student.name();
        int age = student.age();
        return studentService.addNewStudent(name, age);
    }
    
    //...
}
```

If any of the mappings use a service method which throws the NoSuchIdException, then this is also handled in the controller.
It will return a HTTP 404 not found status code and the message defined in the exception handler:

```java
@RestController
@RequestMapping(path = "/students")
public class StudentController {
    //...
    @ResponseBody
    @ExceptionHandler(NoSuchIdException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String studentNotFoundHandler(NoSuchIdException ex) {
        return ex.getMessage();
    }
}
```

The  [NoSuchIdException class](src/main/java/com/khanivorous/studentservice/student/NoSuchIdException.java) simply extends RuntimeException and I have overridden the message to be more meaningful for the error I want to display:

```java
public class NoSuchIdException extends RuntimeException {

    public NoSuchIdException(long id) {
        super("Could not find student with id " + id);
    }

}
```

### Request validation

We use `spring-boot-starter-validation` library to validate the client requests. We can see post request to validate the post request body.  
Here we have added some rules via annotation on the `StudentCreationDTO`.  
We mark the following rules:  
* The name field must not be empty
* Age must be a minimum of 17
* Age must not be null

```java
public record StudentCreationDTO(
        @NotEmpty(message = "name must not be empty")
        String name,
        @NotNull(message = "age must not be null")
        @Min(value = 17, message = "age cannot be less than 17 years old")
        Integer age) {
}
```

We then tell the controller to validate this with the `@Valid` annotation:

```java
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public @ResponseBody
    StudentDTO addNewStudent(@Valid @RequestBody StudentCreationDTO student) {
        String name = student.name();
        int age = student.age();
        return studentService.addNewStudent(name, age);
    }
```

Finally, instead of using the default response json, we configure it ourselves with an ExceptionHandler:  

```java
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
```

## Testing
There are 5 test classes in this repository which I use to demonstrate various spring tests tools/annotations, along with Mockito in order to test a simple spring boot application.

You will notice that there is significant _overtesting_, which in a real project we _should not_ do, but serves the purpose of demonstrating some of the testing tools available in spring.

Some of these are spring specific and some are to do with the Mockito mocking library itself, which can be used for any java library but is obviously very useful here too

The 6 test classes in this repository are

- [StudentServiceTest](src/test/java/com/khanivorous/studentservice/servicetests/StudentServiceTest.java)
- [StudentApplicationTest](src/test/java/com/khanivorous/studentservice/applicationtests/StudentApplicationTest.java)
- [StudentControllerWithServiceMockTests](src/test/java/com/khanivorous/studentservice/controllertests/StudentControllerWithServiceMockTests.java)
- [StudentControllerWithRepositoryMockTests](src/test/java/com/khanivorous/studentservice/controllertests/StudentControllerWithRepositoryMockTests.java)
- [E2ETests](src/test/java/com/khanivorous/studentservice/E2ETests.java)
- [StudentMapperTest](src/test/java/com/khanivorous/studentservice/mappertests/StudentMapperTest.java)


### StudentServiceTest
In this test class we want to specifically test the service itself. Since the service is not responsible for handling the HTTP layer, you will notice there are not tests handling any HTTP responses,
only tests for the methods in the service layer itself.

You will notice that the `StudentService` class itself, has a dependency on `StudentRepository`. 
Testing of this `StudentService` class is made easier by the fact that it uses dependency injection, in this case constructor injection. We can therefore inject a mock/spy of the dependency, being StudentRepository.
Let's take a closer look at this:
```java
@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {
    @Spy
    private StudentRepository studentRepository;
}
```
Here we have annotated `StudentRepository` dependency with `@Spy`. This differs from Mockitos `@Mock` in that `@Spy` uses the actual class
and spies on the methods, allowing you to change only the method you want to change. `@Mock` mocks the hole class and all non-void methods will return null unless otherwise specified.
For these tests, we could easily have also used `@Mock` but I wanted to demonstrate `@Spy` working. 
When you decide to use either `@Mock` or `@Spy`, note that with `@Spy` we are able to test closer to the real thing, but you may have a need to mock the whole dependency, 
therefore needing `@Mock`, or, you may not care about what the dependency does, so again `@Mock` would be better suited.

We have also marked the class with `@ExtendWith(MockitoExtension.class)`, which is a helpful junit 5 extension, which means we do not need to initialise the mocks in a setup method.

Let's now take a closer look at a couple of different tests in this test class.

Firstly, let's look at a method in the service class that we want to test `deleteStudentById(Integer id)`:

```java
import com.khanivorous.studentservice.student.services.StudentService;

@Service
public class StudentServiceImpl implements StudentService {
    //...
    public void deleteStudentById(Integer id) {
        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
        } else {
            throw new NoSuchIdException(id);
        }
    }
    //...
}
```

The following test will test the `true` path of the if statement, i.e. if the Student of the given id exists, we want to delete that student and return a string stating they have been deleted.
Notice that if true, the repository method deleteById(id) is called. It would be useful to check if this has been called without the need to check in a database if it has been deleted. Here's how Mockito can help us. 

Let's now take a look at the actual test:

```java
@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {
    //...
    @Test
    public void testDeleteById() {
        when(studentRepository.existsById(1)).thenReturn(true);
        String response = serviceUnderTest.deleteStudentById(1);
        verify(studentRepository, times(1)).deleteById(1);
    }
    //...
}
```
Firstly, we use `when(studentRepository.existsById(1)).thenReturn(true);` to get the service `deleteStudentById()` method to follow the `true` path of the if statement.
We then assert that the response is "student with id 1 deleted" since we also passed `1` as the parameter.

As stated before, we would expect that if true, the repository method deleteById(id) is called. Mockito has a very useful method `verify`.
Here we check that this method was called 1 time (because we obviously don't want it called multiple times), using the `times()` method.
So, without needing a database and application up and running, we can have confidence that the delete method is called.

What if we want to make sure that a certain method isn't called? For the `deleteStudentById()`, we would like to know that the delete method is _not_ called
if that user does not exist. This is checked in the following test:

```java
public class StudentServiceTest {
    //...
    @Test
    public void testDeleteByNonExistentIdThrowsError() {
        when(studentRepository.existsById(1)).thenReturn(false);
        Exception exception = assertThrows(NoSuchIdException.class, () -> serviceUnderTest.deleteStudentById(1));
        String expectedMessage = "Could not find student with id 1";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
        verify(studentRepository, never()).delete(any());
    }
    //...
}
```
Firstly, we get the `existsById(1)` method to return false, to simulate a non-existent student id.
We then check if the expected exception is thrown. If this is the case, we want to make sure that the delete method is not subsequently called.
Then `never()` method used alongside `verify` from Mockito allows us to check this very case.

Although this is a simple example, this could prove very useful when testing complex systems. 
Imagine a billing process is triggered by your application when a user successfully consumes some resource.
We would not want to bill them if they have not consumed the resource, maybe due to incorrect request. We would want to be able to test as early as possible that this is not triggered.
Mockitos' `verify` method is a very useful tool for these types of use cases.

### StudentControllerTests
The controller handles the incoming HTTP request, uses the service layer to perform the business logic and returns a response to the user,
as well as a HTTP response code, and any exception messages. Although the `StudentController` in this project is not that different to what is in the service class,
its methods are still different and require separate tests. We also can test the HTTP Responses here too.

We _could_ test the controller by spinning up the whole application context and firing rest requests at the endpoints defined in the controller class, then assert on the responses.
But spring lets us do a neat trick and spin up only the controller that we need to test. Within this project there is just one controller class, so it may not seem significant,
but a larger project with more controller classes could benefit from this.

#### StudentControllerWithServiceMockTests
To test a controller by itself, we use the `@WebMvcTest` annotation and mark the controller class/classes we want to test.
Here we do this for the `StudentController` class as follows:

```java
@WebMvcTest(StudentController.class)
public class StudentControllerWithServiceMockTests {
    //...
}
```

Spring gives us a MockMvc client that we can use for testing, by making rest calls and asserting on the response:

```java
@WebMvcTest(StudentController.class)
public class StudentControllerTestsWithServiceMock {
    //...
    @Autowired
    private MockMvc mockMvc;
    //...
}
```
Since the controller has a dependency on the service class, we have mocked out the service as we want to test How the controller responds to the service class responses.
```java
@WebMvcTest(StudentController.class)
public class StudentControllerTestsWithServiceMock {
    //...
    @MockBean
    StudentService studentService;
    //...
}
```

Let's look at a test to get a user by id, but before we do that, let's look at what the controller says for getting a user by id:
```java
@RestController
@RequestMapping(path = "/students")
public class StudentController {
    
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody
    Student getUserById(@PathVariable Integer id) {
        return studentService.getStudentById(id);
    }
}
```
So, the `@RequestMapping` annotation tells us the url path to use. 
The `getUserById()` method is annotated with `@GetMapping` and the `value = "/{id}"` tells us that this method is triggered by a get request to 
the path `"/students/{id}"` and the `produces` tells us that it should respond with a json response.
The `@ResponseStatus(HttpStatus.OK)` tells us that a successful request will result in a HTTP status code of 200 OK.

Notice that the service layer throws the NoSuchIdException and not this controller method. There is an exception handler in the controller class
which tells the controller what to return to the user should the NoSuchIdException be thrown:

```java
@RestController
@RequestMapping(path = "/students")
public class StudentController {
    //...
    @ResponseBody
    @ExceptionHandler(NoSuchIdException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String studentNotFoundHandler(NoSuchIdException ex) {
        return ex.getMessage();
    }
    //...
}
```

When an exception is thrown, the controller will respond with a HTTP status code of 404 Not Found (`@ResponseStatus(HttpStatus.NOT_FOUND)`), and the string message defined in the NoSuchIdException class.

Now, let's look at this test:
```java
@WebMvcTest(StudentController.class)
public class StudentControllerWithServiceMockTests {
    //...
    @Test
    public void testGetUserById() throws Exception {

        Student student1 = new Student();
        student1.setId(1);
        student1.setName("Ben");
        student1.setAge(28);

        when(studentService.getStudentById(1)).thenReturn(student1);

        mockMvc.perform(get("/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Ben")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.age", is(28)));
    }
    //...
}
```
In this test, we mock the service response to respond with a Student object to the controller on a get request with `when(studentService.getStudentById(1)).thenReturn(student1);`.  
The mockMvc client then performs a get request to the path `/students/1"`: `mockMvc.perform(get("/students/1"))`.  
We then assert that the correct HTTP response status is returned: `andExpect(status().isOk())`, followed by checking the contents of the response body:  
`.andExpect(jsonPath("$.name", is("Ben")))`  
`.andExpect(jsonPath("$.id", is(1)))`  
`.andExpect(jsonPath("$.age", is(28)));`  

Now let's look at a test which checks that the correct message and HTTP response status is returned from when the exception is thrown:

```java
@WebMvcTest(StudentController.class)
public class StudentControllerWithServiceMockTests {
    //...
    @Test
    public void testUnknownIdReturnsError() throws Exception {
        doThrow(new NoSuchIdException(1)).when(studentService).deleteStudentById(1);
        mockMvc.perform(get("/students/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Could not find student with id 1"));
    }
    //...
}
```
Here we get the mocked service layer to throw the exception. Now we expect the controller to handle the exception as defined in the `StudentController` class.  
We then check the correct status code is returned: `andExpect(status().isNotFound())` and we check the content of the exception message is as expected: `andExpect(content().string("Could not find student with id 2"))`

#### StudentControllerWithRepositoryMockTests
This test class also tests the StudentController, except we are mocking the repository instead of the service. 
I wanted to show you this just to note different levels of mocking possibilities.  
You will notice the `@ContextConfiguration(classes = {StudentServiceApplication.class, StudentServiceImpl.class})` line at the top of the class.
Because we did not mock the service we have to tell spring to load it into the context along with the `StudentServiceApplication.class`.

### StudentApplicationTest
This test class demonstrates testing the whole application context instead of just a controller.  
If you're application has multiple controllers and you wanted to test whole application, then the `@SpringBootTest` annotation allows
us to do so.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class StudentApplicationTest {
    //...
}
```
Notice the `SpringBootTest.WebEnvironment.RANDOM_PORT` property of the annotation. Here we have set a random port, so when the tests run on a pipeline, or any machine in fact, it will look for a free port to run the tests against.
The MockMvc will be autoconfigured (`@AutoConfigureMockMvc`) to use the correct url and chosen port. There is no need ot do any extra setup. In this class we have still mocked the respository layer as we have not launched any dbs for this test and want control over the data for the tests.

### E2ETests
In this test class, we will launch the whole application and it will use an in memory H2 database as defined in [application.properties file](src/test/resources/application.properties) under the test resources.
SpringBootTests knows to launch those application properties in the test resources. This allows us to do a full end-to-end test of the application with a real database.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class E2ETests {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.basePath = "/students";
        RestAssured.port = port;
    }
}
```
Here, once again, we use the `@SpringBootTest` annotation to load the whole application and we use a RestAssured client which is used for testing RESTful services.  
The `@LocalServerPort` annotation allocates the port number found to this field, which can be used by the RestAssured client with `RestAssured.port = port`.  
The tests themselves are very different to the tests in the other test classes, as we have to write the test in a way that handles real data, as there are no mocks to change the data to test a greater variety of use cases.

### StudentMapperTest

This is a fairly simple set of tests not requiring any mocks. We simply assert the return values of the converter methods.