export default function Table({ columns, rows, onEdit }: { columns: string[]; rows: Record<string, unknown>[]; onEdit?: (row: Record<string, unknown>) => void }) {
  return <div className="ui-table-wrap"><table className="ui-table"><thead><tr>{columns.map(c => <th key={c}>{c}</th>)}{onEdit && <th>Aksi</th>}</tr></thead><tbody>{rows.map((row, i) => <tr key={String(row.id ?? i)}>{columns.map(c => <td key={c}>{String(row[c] ?? '-')}</td>)}{onEdit && <td><button className="table-edit" onClick={() => onEdit(row)}>Edit</button></td>}</tr>)}</tbody></table></div>
}
