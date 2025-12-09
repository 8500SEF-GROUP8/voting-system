## Features

*   **User Authentication:** Secure user registration and login using JWT (JSON Web Tokens).
*   **Vote Management:**
    *   Create new votes with a title, description, and multiple options.
    *   Edit votes while they are in a `DRAFT` status.
    *   Publish votes to make them available for participation.
    *   Close votes to stop further participation.
    *   Delete votes (soft delete by changing status).
*   **Vote Permissions:**
    *   **PUBLIC:** Visible to all authenticated users.
    *   **PRIVATE:** Visible only to the creator.
    *   **LINK_ONLY:** Accessible only via a unique shareable link.
*   **Participation:** Authenticated users can cast one vote per poll. Anonymous participation is supported for `LINK_ONLY` polls.
*   **Real-time Results:** View vote counts and percentages for each option on any accessible poll.

## Setup and Installation

### Prerequisites

*   Java Development Kit (JDK) 21 or later.
*   Apache Maven.

### Running the Application

1.  **Clone the repository:**
    ```sh
    git clone <repository-url>
    cd voting-system
    ```

2.  **Build the project:**
    This command will compile the source code and package it into a `.jar` file.
    ```sh
    mvn clean install
    ```

3.  **Run the application:**
    This will start the Spring Boot server.
    ```sh
    mvn spring-boot:run
    ```

The application will be running at `http://localhost:8080`.

## Running Tests

To run the suite of automated unit tests, execute the following command:

```sh
mvn test
```