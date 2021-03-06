package at.jku.ssw.zestexample.view;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.widgets.ZestStyles;

import at.jku.ssw.zestexample.ZestTreeExamplePlugin;
import at.jku.ssw.zestexample.testmodel.TestElem;
import at.jku.ssw.zestexample.testmodel.TestElemConnectionStyleProvider;
import at.jku.ssw.zestexample.testmodel.TestElemContentProvider;
import at.jku.ssw.zestexample.testmodel.TestElemEntityStyleProvider;
import at.jku.ssw.zestexample.testmodel.TestElemLabelProvider;
import at.jku.ssw.zestexample.testmodel.TestModel;
import at.jku.ssw.zesttree.VisualTreeViewer;
import at.jku.ssw.zesttree.providers.Tree2GraphProvider;

import org.eclipse.zest.core.viewers.*;

/**
 * Zest Example Family Tree View
 */
public class ZestViewOne extends ViewPart {

	private Image[] images;

	public ZestViewOne() {

		super();
	}

	@Override
	public void createPartControl(Composite parent) {

		TestElem model = createTestModel();

	//	VisualTreeViewer t = new VisualTreeViewer(parent, 2);
		GraphViewer t = new GraphViewer(parent, ZestStyles.NONE); 
		//t.getInternViewer().setNodeStyle(ZestStyles.NODES_HIDE_TEXT);

		Tree2GraphProvider adapter = new Tree2GraphProvider(
				new TestElemContentProvider(), new TestElemLabelProvider(),
				new TestElemEntityStyleProvider(),
				new TestElemConnectionStyleProvider());

		t.setContentProvider(adapter);
		t.setLabelProvider(adapter);
		t.setInput(model);
	}

	@Override
	public void setFocus() {

	}

	private TestElem createTestModel() {

		images = new Image[] { getImage("/icons/g1.gif"),
				getImage("/icons/g21.gif"), getImage("/icons/g22.gif"),
				getImage("/icons/g23.gif"), getImage("/icons/g24.gif"),
				getImage("/icons/g25.gif"), getImage("/icons/g26.gif"),
				getImage("/icons/g31.gif"), getImage("/icons/g32.gif"),
				getImage("/icons/g33.gif"), getImage("/icons/g34.gif"),
				getImage("/icons/g35.gif"), getImage("/icons/g36.gif"),
				getImage("/icons/g37.gif"), getImage("/icons/g38.gif"),
				getImage("/icons/g39.gif"), getImage("/icons/g310.gif"),
				getImage("/icons/g311.gif"), getImage("/icons/g41.gif"),
				getImage("/icons/g42.gif"), getImage("/icons/g43.gif"),
				getImage("/icons/g44.gif"), getImage("/icons/g45.gif"),
				getImage("/icons/g46.gif"), getImage("/icons/g47.gif"),
				getImage("/icons/g48.gif"), getImage("/icons/g49.gif"),
				getImage("/icons/g410.gif"), getImage("/icons/g411.gif"),
				getImage("/icons/g412.gif"), getImage("/icons/g413.gif"),
				getImage("/icons/g414.gif"), getImage("/icons/g51.gif"),
				getImage("/icons/g52.gif"), getImage("/icons/g53.gif"),
				getImage("/icons/g54.gif"), getImage("/icons/g55.gif") };

		return new TestModel("Root", images, true);
	}

	private static Image getImage(final String path) {

		try {
			ImageDescriptor i = ZestTreeExamplePlugin.getImageDescriptor(path);

			return ImageDescriptor.createFromImageData(
					i.getImageData().scaledTo(16, 16)).createImage();
//			return i.createImage();
		} catch(Exception e) {
			return null;
		}
	}
}
