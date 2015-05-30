package bart.model.errorgenerator.operator;

import bart.BartConstants;
import bart.exceptions.DAOException;
import bart.model.algebra.operators.ITupleIterator;
import bart.model.database.Attribute;
import bart.model.database.AttributeRef;
import bart.model.database.Cell;
import bart.model.database.IDatabase;
import bart.model.database.ITable;
import bart.model.database.IValue;
import bart.model.database.Tuple;
import bart.model.errorgenerator.CellChanges;
import bart.utility.BartUtility;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportDatabaseCSV implements IExportDatabase {

    private static Logger logger = LoggerFactory.getLogger(ExportDatabaseCSV.class);
    private static String SEPARATOR = ",";
    private static String NEW_LINE = "\n";

    public void export(IDatabase database, String path) {
        export(database, new CellChanges(), path);
    }

    public void export(IDatabase database, CellChanges cellChanges, String path) {
        if (logger.isDebugEnabled()) logger.debug("Exporting database to path " + path);
        for (String tableName : database.getTableNames()) {
            ITable table = database.getTable(tableName);
            exportTable(table, cellChanges, path);
        }
    }

    private void exportTable(ITable table, CellChanges cellChanges, String path) {
        path += File.separator + "dirty_" + table.getName() + ".csv";
        Writer out = null;
        try {
            File outFile = new File(path);
            outFile.getParentFile().mkdirs();
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
            out.write(writeHeader(table));
            ITupleIterator it = table.getTupleIterator();
            while (it.hasNext()) {
                Tuple tuple = it.next();
                out.write(writeTuple(tuple, table, cellChanges));
            }
        } catch (Exception ex) {
            logger.error("Unable to export cell changes to path " + path + "\n\t" + ex.getLocalizedMessage());
            throw new DAOException(ex);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    private String writeHeader(ITable table) {
        StringBuilder sb = new StringBuilder();
        for (Attribute attribute : getAttributes(table)) {
            sb.append(attribute.getName());
            sb.append(SEPARATOR);
        }
        BartUtility.removeChars(SEPARATOR.length(), sb);
        sb.append(NEW_LINE);
        return sb.toString();
    }

    private String writeTuple(Tuple tuple, ITable table, CellChanges cellChanges) {
        StringBuilder sb = new StringBuilder();
        for (Attribute attribute : getAttributes(table)) {
            Cell cell = tuple.getCell(new AttributeRef(attribute.getTableName(), attribute.getName()));
            IValue value;
            if (cellChanges.cellHasBeenChanged(cell)) {
                value = cellChanges.getNewValue(cell);
            } else {
                value = cell.getValue();
            }
            sb.append(writeValue(value));
            sb.append(SEPARATOR);
        }
        BartUtility.removeChars(SEPARATOR.length(), sb);
        sb.append(NEW_LINE);
        return sb.toString();
    }

    private String writeValue(IValue value) {
        if(value == null){
            return "";
        }
        String s = value.toString();
        if(s.contains(SEPARATOR)){
            logger.warn("Removing csv separator value " + SEPARATOR + " from " + s);
            s = s.replaceAll(SEPARATOR, "");
        }
        return s;
    }
    
    private List<Attribute> getAttributes(ITable table){
        List<Attribute> result = new ArrayList<Attribute>();
        for (Attribute attribute : table.getAttributes()) {
            if(attribute.getName().equalsIgnoreCase(BartConstants.OID)){
                continue;
            }
            result.add(attribute);
        }
        return result;
    }

}