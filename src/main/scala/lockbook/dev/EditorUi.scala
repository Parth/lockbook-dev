package lockbook.dev

import java.io.File

import com.vladsch.flexmark.ast._
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.{Node, NodeVisitor, VisitHandler}
import javafx.application.Platform
import javafx.scene.control._
import javafx.scene.layout.{BorderPane, HBox}
import org.eclipse.jgit.api.Git
import org.fxmisc.richtext.CodeArea

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EditorUi(editorHelper: EditorHelper) {

  def getView(git: Git, f: File): BorderPane = {
    val textArea = new CodeArea()
    val root     = new BorderPane

    loadFile(git, f, root, textArea)

    root
  }

  private def loadFile(git: Git, f: File, root: BorderPane, text: CodeArea): Future[Unit] = Future {
    editorHelper.getTextFromFile(f) match {
      case Right(fileText) =>
        Platform.runLater(() => {
          doMarkdown(text)
          text.setWrapText(true)
          root.setBottom(getBottom(git, f, text))
          root.setCenter(text)
          text.replaceText(fileText)
        })
      case Left(error) =>
        Platform.runLater(
          () => root.setCenter(new Label(error.uiMessage))
        )
    }
  }

  private def doMarkdown(text: CodeArea): Unit = {
    val parser: Parser = Parser.builder().build()

    text
      .textProperty()
      .addListener((_, _, newText) => {
        val parsed = parser.parse(newText)
        nodeVisitor(text).visit(parsed)
      })
  }

  private def getBottom(git: Git, file: File, textArea: CodeArea): HBox = {
    val save          = new Button("Push")
    val commitMessage = new TextField
    commitMessage.setPromptText("Commit Message")

    save.setOnAction(_ => {      Future {
        editorHelper
          .saveCommitAndPush(commitMessage.getText, textArea.getText, file, git) match {
          case Right(_) =>
            Platform.runLater(() => {
              AlertUi.showGood("Push Successful", "Changes saved successfully.")
            })
          case Left(exception) =>
            Platform.runLater(() => {
              AlertUi.showBad("Push Failed", exception.uiMessage)
            })
        }
      }
    })

    new HBox(commitMessage, save)
  }

  private val nodeVisitor: CodeArea => NodeVisitor = (styledText: CodeArea) =>
    new NodeVisitor(new VisitHandler[Text](classOf[Text], (node: Text) => {
      styledText.setStyleClass(node.getStartOffset, node.getEndOffset, "text")
      setParagraphStyle(styledText, node, "inline")
    }), new VisitHandler[Heading](classOf[Heading], (node: Heading) => {
      styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getText.getEndOffset, s"h${node.getLevel}")
      setParagraphStyle(styledText, node, "inline")
    }), new VisitHandler[Code](classOf[Code], (node: Code) => {
      styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "code")
      setParagraphStyle(styledText, node, "inline")
    }), new VisitHandler[Emphasis](classOf[Emphasis], (node: Emphasis) => {
      styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "emphasis")
      setParagraphStyle(styledText, node, "inline")
    }), new VisitHandler[FencedCodeBlock](classOf[FencedCodeBlock], (node: FencedCodeBlock) => {
      if (node.getClosingFence.getEndOffset != 0) {
        styledText.setStyleClass(node.getOpeningMarker.getStartOffset, node.getClosingMarker.getEndOffset, "code-block")
        setParagraphStyle(styledText, node, "code-block")
      }
    }), new VisitHandler[BlockQuote](classOf[BlockQuote], (node: BlockQuote) => {
      styledText.setStyleClass(node.getStartOffset, node.getEndOffset, "quote-block")
      setParagraphStyle(styledText, node, "quote-block")
    }), new VisitHandler[Link](classOf[Link], (node: Link) => {
      styledText.setStyleClass(node.getTextOpeningMarker.getStartOffset+1, node.getTextClosingMarker.getEndOffset-1, "link")
      styledText.setStyleClass(node.getLinkOpeningMarker.getStartOffset+1, node.getLinkClosingMarker.getEndOffset-1, "href")
      setParagraphStyle(styledText, node, "inline")
    }) )

  private def setParagraphStyle(styledText: CodeArea, node: Node, style: String) = {
    Array
      .range(node.getStartLineNumber, node.getEndLineNumber + 1)
      .foreach(styledText.setParagraphStyle(_, List(style).asJava))
  }
}
