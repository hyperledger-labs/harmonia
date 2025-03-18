import * as React from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';


type AssetRow = {
    assetName: string,
    txHash: string,
}
type Props = {
    assetRows: AssetRow[]
}

export default function AssetsTable({assetRows}:Props) {
    const inverted = assetRows.reverse()
  return (
    <TableContainer component={Paper}>
      <Table sx={{ minWidth: 650 }} aria-label="simple table">
        <TableHead>
          <TableRow>
            <TableCell>Asset Name</TableCell>
            <TableCell align="right">TX Hash</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {inverted.map((row) => (
            <TableRow
              key={row.txHash}
              sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
            >
                              <TableCell component="th" scope="row">
                {row.assetName}
              </TableCell>
              <TableCell component="th" scope="row" align="right">
                {row.txHash.substring(0, 20)}...
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
