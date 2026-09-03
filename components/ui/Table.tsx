export default function Table({ columns, rows }: { columns: string[]; rows: Record<string, unknown>[] }) {
  return <div className="ui-table-wrap"><table className="ui-table"><thead><tr>{columns.map(c => <th key={c}>{c}</th>)}</tr></thead><tbody>{rows.map((row, i) => <tr key={String(row.id ?? i)}>{columns.map(c => <td key={c}>{String(row[c] ?? '-')}</td>)}</tr>)}</tbody></table></div>
}
