F0: Basic Functionalities (For All Groups)
Design and develop an IntelliJ IDEA plugin that serves as a teaching assistant (TA) of the Java 
Enterprise Application Development course.
l Implement a course-specific Q&A chatbot with retrieval-augmented generation (RAG) and 
source citation, which acts as the TA. It pre-processes course materials (such as lecture slides), 
and breaks them down into searchable knowledge chunks and indexes them. Whenever the
user asks a coding-related question, the plugin should retrieve the most relevant chunks from 
the knowledge base. The retrieved chunks are then provided as specific and contextual 
information to an LLM service along with the original question. This process enables the TA 
to generate a sophisticated response that combines general Java programming knowledge with 
specific and verifiable information from the course materials.
n As a critical part of this pipeline, for any response generated based on the retrieved course 
material, the TA must clearly cite the source document and page number/scope. If no 
relevant material is found and the answer has to be generated based on the LLM’s general 
knowledge, the TA should explicitly state such situation (e.g., “Response is based on 
general knowledge; no specific course material is referenced”).
l User Interface: Create a user-friendly interface that is embedded in the IntelliJ IDEA (e.g. 
sidebar or tool window) for interacting with the TA.
l Context Awareness: The plugin must be aware of the user’s programming context when generating answers. At a minimum, implement a right-click context menu action in the editor 
to allow users to quickly ask questions related to the selected code segment


F2: Extended Feature 2 (For Group C Only)
In addition to the basic requirements, this feature focuses on creating a professional user experience 
for AI-driven source code modification. The system should provide a feature that can perform 
significant code modification tasks according to the user’s instructions, such as refactoring a method 
by applying a design pattern or fixing a bug in the source code. When modifying the source code, 
the plugin must firstly display the suggested changes using IntelliJ IDEA’s built-in diff viewer, 
which provides a clear and side-by-side comparison and allows the user to review and approve the 
AI-generated suggestions before applying them